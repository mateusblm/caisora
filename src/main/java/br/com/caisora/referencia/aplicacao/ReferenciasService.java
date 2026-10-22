package br.com.caisora.referencia.aplicacao;

import br.com.caisora.referencia.api.ReferenciasResponse.*;
import br.com.caisora.referencia.infraestrutura.ibge.IbgeClient;
import br.com.caisora.referencia.infraestrutura.brasilapi.BrasilApiClient;
import br.com.caisora.referencia.infraestrutura.worldtides.WorldTidesClient;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ReferenciasService {
    private static final Set<String> UFS = Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO");
    private final IbgeClient ibge;
    private final BrasilApiClient brasil;
    private final WorldTidesClient tides;
    private final OrganizacaoService organizacoes;
    public ReferenciasService(IbgeClient ibge, BrasilApiClient brasil, WorldTidesClient tides, OrganizacaoService organizacoes) {
        this.ibge=ibge; this.brasil=brasil; this.tides=tides; this.organizacoes=organizacoes;
    }
    public Consulta<Pais> paises(String busca) {
        String filtro = busca(busca);
        var lista = proteger(() -> ibge.paises());
        boolean incompleta = lista.stream().anyMatch(p -> p.nome()==null || p.codigoM49()==null || p.codigoIso2()==null || p.codigoIso3()==null);
        var filtrados = lista.stream().filter(p -> contem(p.nome(),filtro)||contem(p.codigoIso2(),filtro)||contem(p.codigoIso3(),filtro)||contem(p.codigoM49()==null?null:p.codigoM49().toString(),filtro)).toList();
        return new Consulta<>("IBGE — Países", "https://servicodados.ibge.gov.br/api/docs/paises", Instant.now(), incompleta, filtrados);
    }
    public Consulta<Municipio> municipios(String uf, String busca) {
        String estado = uf(uf, true), filtro = busca(busca);
        var lista = proteger(() -> ibge.municipios(estado));
        boolean incompleta = lista.stream().anyMatch(m -> m.nome()==null || m.codigoIbge()==null || m.siglaUf()==null || m.codigoUf()==null || m.nomeUf()==null);
        var filtrados = lista.stream().filter(m -> contem(m.nome(),filtro)||contem(m.codigoIbge()==null?null:m.codigoIbge().toString(),filtro)).toList();
        return new Consulta<>("IBGE — Localidades", "https://servicodados.ibge.gov.br/api/docs/localidades", Instant.now(), incompleta, filtrados);
    }
    public Consulta<Feriado> feriados(int ano, String uf) {
        if (ano<1900 || ano>2199) throw invalido("Ano deve estar entre 1900 e 2199");
        String estado = uf(uf,false);
        var lista = proteger(() -> brasil.feriados(ano,estado));
        boolean incompleta = lista.stream().anyMatch(f -> f.data()==null || f.nome()==null || f.tipo()==null || ("state".equals(f.tipo())&&f.uf()==null));
        return new Consulta<>("BrasilAPI — Feriados", "https://brasilapi.com.br/docs#tag/Feriados-Nacionais", Instant.now(), incompleta, lista);
    }
    public DisponibilidadeMares disponibilidadeMares() {
        return new DisponibilidadeMares(tides.disponivel(), tides.disponivel()
                ? "Consulta consome créditos WorldTides conforme o plano autorizado pela plataforma"
                : "WorldTides desabilitada. A plataforma deve fornecer credenciais e autorizar o uso antes de consumir créditos");
    }
    public Mares mares(UUID organizacaoId, LocalDate inicio, int dias) {
        var hoje = LocalDate.now(ZoneOffset.UTC);
        if (inicio==null || inicio.isBefore(hoje) || inicio.isAfter(hoje.plusDays(6)) || dias<1 || dias>7
                || inicio.plusDays(dias-1).isAfter(hoje.plusDays(6))) throw invalido("Escolha de 1 a 7 dias no intervalo de hoje aos próximos 6 dias");
        if (!tides.disponivel()) throw new DadosInvalidosException("MARES_DESABILITADAS", disponibilidadeMares().mensagem());
        var coordenadas = organizacoes.coordenadas(organizacaoId);
        if (coordenadas.latitude()==null || coordenadas.longitude()==null) throw invalido("Configure as coordenadas da marina");
        return proteger(() -> tides.consultar(coordenadas.latitude(),coordenadas.longitude(),inicio,dias));
    }
    private <T> T proteger(java.util.function.Supplier<T> consulta) {
        try { return consulta.get(); }
        catch (ReferenciaIndisponivelException e) { throw e; }
        catch (RuntimeException e) { throw new ReferenciaIndisponivelException(); }
    }
    private String busca(String valor) {
        if (valor!=null && valor.length()>100) throw invalido("Pesquisa deve ter até 100 caracteres");
        return valor==null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
    private boolean contem(String valor,String busca) { return busca.isEmpty() || (valor!=null && valor.toLowerCase(Locale.ROOT).contains(busca)); }
    private String uf(String valor, boolean obrigatoria) {
        if (valor==null || valor.isBlank()) { if (obrigatoria) throw invalido("Informe a UF"); return null; }
        String uf=valor.trim().toUpperCase(Locale.ROOT);
        if (!UFS.contains(uf)) throw invalido("UF inválida");
        return uf;
    }
    private DadosInvalidosException invalido(String mensagem) { return new DadosInvalidosException("FILTRO_REFERENCIA_INVALIDO", mensagem); }
}
