package br.com.caisora.relatorio.aplicacao;

import br.com.caisora.relatorio.api.RelatoriosResponse.*;
import br.com.caisora.relatorio.api.PermanenciaVagasResponse;
import br.com.caisora.relatorio.dominio.DadosRelatorio;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.vaga.dominio.*;
import br.com.caisora.movimentacao.dominio.*;
import br.com.caisora.contrato.dominio.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class RelatorioService {
    private final DadosRelatorio dados;
    public RelatorioService(DadosRelatorio dados) {
        this.dados=dados;
    }
    public Ocupacao ocupacao(UUID org,String setor,TipoVaga tipo) {
        if(setor!=null && setor.length()>100) throw new DadosInvalidosException("FILTRO_INVALIDO","Setor deve ter até 100 caracteres");
        var vagas=dados.vagas(org,setor==null||setor.isBlank()?null:setor.trim(),tipo);
        record Chave(String setor,TipoVaga tipo) {
        }
        var grupos=vagas.stream().collect(Collectors.groupingBy(v->new Chave(v.setor(),v.tipo())));
        var resultado=grupos.entrySet().stream().map(e-> {
            long ocupadas=e.getValue().stream().filter(DadosRelatorio.VagaOcupada::ocupada).count();
            long total=e.getValue().size();
            return new GrupoOcupacao(e.getKey().setor(),e.getKey().tipo(),total,ocupadas,total-ocupadas,percentual(ocupadas,total));
        }
        ).sorted(Comparator.comparing(GrupoOcupacao::setor,Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(g->g.tipo().name())).toList();
        long ocupadas=vagas.stream().filter(DadosRelatorio.VagaOcupada::ocupada).count();
        return new Ocupacao(vagas.size(),ocupadas,vagas.size()-ocupadas,percentual(ocupadas,vagas.size()),resultado);
    }
    public Movimentacoes movimentacoes(UUID org,LocalDate inicio,LocalDate fim,TipoMovimentacao tipo) {
        validarPeriodo(inicio,fim,true);
        var lista=dados.movimentacoes(org,inicio.atStartOfDay(ZoneOffset.UTC).toInstant(),fim.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),tipo);
        Map<StatusMovimentacao,Long> contagem=new EnumMap<>(StatusMovimentacao.class);
        for(var status:StatusMovimentacao.values()) contagem.put(status,0L);
        BigDecimal duracao=BigDecimal.ZERO;
        long amostras=0,pontuais=0;
        for(var m:lista) {
            contagem.merge(m.getStatus(),1L,Long::sum);
            if(m.getStatus()==StatusMovimentacao.CONCLUIDA) {
                if(m.getConcluidaEm()!=null && m.getAgendadaPara()!=null && !m.getConcluidaEm().isAfter(m.getAgendadaPara())) pontuais++;
                if(m.getIniciadaEm()!=null && m.getConcluidaEm()!=null && !m.getConcluidaEm().isBefore(m.getIniciadaEm())) {
                    duracao=duracao.add(BigDecimal.valueOf(Duration.between(m.getIniciadaEm(),m.getConcluidaEm()).toMillis(),3));
                    amostras++;
                }
            }
        }
        long concluidas=contagem.get(StatusMovimentacao.CONCLUIDA);
        return new Movimentacoes(lista.size(),contagem,concluidas,contagem.get(StatusMovimentacao.CANCELADA),
        amostras==0?null:duracao.divide(BigDecimal.valueOf(amostras),2,RoundingMode.HALF_UP),amostras,pontuais,percentual(pontuais,concluidas),
        "Pontual quando concluidaEm <= agendadaPara, sem tolerância. Período pelo agendamento em UTC. Todas as concluídas compõem o denominador.");
    }
    public Contratos contratos(UUID org,StatusContrato status,PeriodicidadeContrato periodicidade,LocalDate inicio,LocalDate fim) {
        validarPeriodo(inicio,fim,false);
        var lista=dados.contratos(org,status,periodicidade,inicio,fim);
        Map<StatusContrato,Long> situacoes=new EnumMap<>(StatusContrato.class);
        Map<StatusContrato,BigDecimal> percentuais=new EnumMap<>(StatusContrato.class);
        Map<PeriodicidadeContrato,Long> periodos=new EnumMap<>(PeriodicidadeContrato.class);
        Map<PeriodicidadeContrato,BigDecimal> valores=new EnumMap<>(PeriodicidadeContrato.class);
        for(var s:StatusContrato.values()) situacoes.put(s,0L);
        for(var p:PeriodicidadeContrato.values()) {
            periodos.put(p,0L);
            valores.put(p,BigDecimal.ZERO);
        }
        for(var c:lista) {
            situacoes.merge(c.getStatus(),1L,Long::sum);
            periodos.merge(c.getPeriodicidade(),1L,Long::sum);
            valores.merge(c.getPeriodicidade(),c.getValorBase(),BigDecimal::add);
        }
        situacoes.forEach((s,n)->percentuais.put(s,percentual(n,lista.size())));
        return new Contratos(lista.size(),situacoes,percentuais,periodos,valores);
    }
    public PermanenciaVagasResponse permanenciaVagas(
            UUID org, LocalDate inicio, LocalDate fim, String setor, TipoVaga tipo
    ) {
        validarPeriodo(inicio, fim, true);
        if (inicio.getYear() < 1 || fim.getYear() > 9999) {
            throw new DadosInvalidosException("PERIODO_INVALIDO", "As datas devem estar entre os anos 1 e 9999");
        }
        if (setor != null && setor.length() > 100) {
            throw new DadosInvalidosException("FILTRO_INVALIDO", "Setor deve ter até 100 caracteres");
        }
        String setorNormalizado = setor == null || setor.isBlank() ? null : setor.trim();
        var ocupacoes = dados.permanencias(org, inicio.atStartOfDay(ZoneOffset.UTC).toInstant(),
                fim.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), setorNormalizado, tipo);
        record Chave(String setor, TipoVaga tipo) {}
        Map<Chave, AcumuladorPermanencia> grupos = new HashMap<>();
        var total = new AcumuladorPermanencia();
        for (var ocupacao : ocupacoes) {
            if (ocupacao.inicioEm() == null || ocupacao.encerradaEm() == null || ocupacao.tipo() == null
                    || ocupacao.encerradaEm().isBefore(ocupacao.inicioEm())) {
                throw new DadosInvalidosException("DADOS_HISTORICOS_INVALIDOS",
                        "Ocupação encerrada com dados ausentes ou cronologia inválida; revise os registros históricos");
            }
            var duracao = Duration.between(ocupacao.inicioEm(), ocupacao.encerradaEm());
            // Preserva a fração de segundo; arredonda somente os indicadores apresentados.
            var segundos = BigDecimal.valueOf(duracao.getSeconds())
                    .add(BigDecimal.valueOf(duracao.getNano(), 9));
            total.adicionar(segundos);
            grupos.computeIfAbsent(new Chave(ocupacao.setor(), ocupacao.tipo()),
                    chave -> new AcumuladorPermanencia()).adicionar(segundos);
        }
        var agrupados = grupos.entrySet().stream()
                .map(entrada -> new PermanenciaVagasResponse.Grupo(entrada.getKey().setor(),
                        entrada.getKey().tipo(), entrada.getValue().indicadores()))
                .sorted(Comparator.comparing(PermanenciaVagasResponse.Grupo::setor,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(grupo -> grupo.tipo().name()))
                .toList();
        return new PermanenciaVagasResponse(total.indicadores(), agrupados);
    }

    private static final class AcumuladorPermanencia {
        private long quantidade;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal menor;
        private BigDecimal maior;

        void adicionar(BigDecimal segundos) {
            quantidade++;
            total = total.add(segundos);
            menor = menor == null ? segundos : menor.min(segundos);
            maior = maior == null ? segundos : maior.max(segundos);
        }

        PermanenciaVagasResponse.Indicadores indicadores() {
            return new PermanenciaVagasResponse.Indicadores(quantidade,
                    quantidade == 0 ? null : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP),
                    menor == null ? null : menor.setScale(2, RoundingMode.HALF_UP),
                    maior == null ? null : maior.setScale(2, RoundingMode.HALF_UP),
                    total.divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP));
        }
    }

    private void validarPeriodo(LocalDate inicio,LocalDate fim,boolean obrigatorio) {
        if((obrigatorio && (inicio==null||fim==null)) || (inicio!=null&&fim!=null&&fim.isBefore(inicio)) || (fim!=null&&fim.equals(LocalDate.MAX)))
        throw new DadosInvalidosException("PERIODO_INVALIDO","Período inválido: informe datas em ordem cronológica");
    }
    private BigDecimal percentual(long parte,long total) {
        return total==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(parte).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);
    }
}
