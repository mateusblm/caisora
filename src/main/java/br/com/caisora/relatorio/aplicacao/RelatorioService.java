package br.com.caisora.relatorio.aplicacao;

import br.com.caisora.relatorio.api.RelatoriosResponse.*;
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
    private void validarPeriodo(LocalDate inicio,LocalDate fim,boolean obrigatorio) {
        if((obrigatorio && (inicio==null||fim==null)) || (inicio!=null&&fim!=null&&fim.isBefore(inicio)) || (fim!=null&&fim.equals(LocalDate.MAX)))
        throw new DadosInvalidosException("PERIODO_INVALIDO","Período inválido: informe datas em ordem cronológica");
    }
    private BigDecimal percentual(long parte,long total) {
        return total==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(parte).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);
    }
}
