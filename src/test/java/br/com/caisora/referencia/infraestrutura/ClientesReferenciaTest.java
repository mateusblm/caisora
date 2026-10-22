package br.com.caisora.referencia.infraestrutura;

import static org.assertj.core.api.Assertions.*;
import br.com.caisora.referencia.infraestrutura.http.TransporteJsonExterno;
import br.com.caisora.referencia.infraestrutura.ibge.IbgeClient;
import br.com.caisora.referencia.infraestrutura.brasilapi.BrasilApiClient;
import br.com.caisora.referencia.infraestrutura.worldtides.WorldTidesClient;
import br.com.caisora.referencia.aplicacao.ReferenciaIndisponivelException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientesReferenciaTest {
    private ServidorReferencias servidor;
    private TransporteJsonExterno http;
    @BeforeEach void iniciar() throws Exception {servidor=new ServidorReferencias();http=new TransporteJsonExterno(500,500);}
    @AfterEach void encerrar(){servidor.close();}
    @Test void paisesLeemIdentificadoresReaisETratamGzip() throws Exception {
        servidor.corpo=ServidorReferencias.fixture("paises");servidor.gzip=true;
        var paises=new IbgeClient(http,servidor.url()).paises();
        assertThat(paises).hasSize(2);assertThat(paises.getFirst().nome()).isEqualTo("Brasil");
        assertThat(paises.getFirst().codigoM49()).isEqualTo(76);assertThat(paises.getFirst().codigoIso2()).isEqualTo("BR");
        assertThat(servidor.ultimaUri).isEqualTo("/api/v1/paises/-");
    }
    @Test void municipiosSuportamAsDuasEstruturasDeRegiaoSemInventarUf() throws Exception {
        servidor.corpo=ServidorReferencias.fixture("municipios");var lista=new IbgeClient(http,servidor.url()).municipios("SC");
        assertThat(lista).hasSize(2);assertThat(lista).allMatch(m->"SC".equals(m.siglaUf()));
        assertThat(lista.getFirst().codigoIbge()).isEqualTo(4205407);assertThat(lista.get(1).nomeUf()).isEqualTo("Santa Catarina");
        assertThat(servidor.ultimaUri).isEqualTo("/api/v1/localidades/estados/SC/municipios");
    }
    @Test void brasilApiEncaminhaUfEPreservaTiposENaoCalculaDiaAusente() throws Exception {
        servidor.corpo=ServidorReferencias.fixture("feriados");var lista=new BrasilApiClient(http,servidor.url()).feriados(2026,"SC");
        assertThat(lista.getFirst().tipo()).isEqualTo("national");assertThat(lista.get(1).tipo()).isEqualTo("state");
        assertThat(lista.get(1).uf()).isEqualTo("SC");assertThat(lista.get(1).diaSemana()).isNull();
        assertThat(servidor.ultimaUri).isEqualTo("/api/feriados/v1/2026?uf=SC");
    }
    @Test void worldTidesUsaApenasExtremosEExibeDatumCopyrightEUnidadeDoContrato() throws Exception {
        servidor.corpo=ServidorReferencias.fixture("worldtides");var r=tides(true,true,"chave-teste").consultar(BigDecimal.ZERO,BigDecimal.TEN,LocalDate.of(2026,10,22),1);
        assertThat(r.registros()).hasSize(2);assertThat(r.registros().getFirst().tipo()).isEqualTo("High");
        assertThat(r.registros().getFirst().unidade()).isEqualTo("m");assertThat(r.registros().get(1).altura()).isEqualTo(-0.3);
        assertThat(r.registros().getFirst().dataHora()).isEqualTo(Instant.parse("2026-10-22T00:00:00Z"));
        assertThat(r.datumResposta()).isEqualTo("MSL");assertThat(r.copyright()).contains("CONTROLADA");
        assertThat(servidor.ultimaUri).contains("extremes", "days=1","key=chave-teste","datum=MSL").doesNotContain("heights");
    }
    @Test void credenciaisSozinhasNaoHabilitamWorldTides(){
        for(var cliente:java.util.List.of(tides(false,true,"chave"),tides(true,false,"chave"),tides(true,true,""))){
            assertThat(cliente.disponivel()).isFalse();assertThatThrownBy(()->cliente.consultar(BigDecimal.ZERO,BigDecimal.ZERO,LocalDate.now(),1)).isInstanceOf(DadosInvalidosException.class);
        }assertThat(servidor.chamadas).hasValue(0);
    }
    @Test void erroWorldTidesNoCorpoECopyrightAusenteSaoRecusados(){
        servidor.corpo="{\"status\":400,\"error\":\"chave-teste\"}";
        assertThatThrownBy(()->tides(true,true,"chave-teste").consultar(BigDecimal.ZERO,BigDecimal.ZERO,LocalDate.now(),1)).isInstanceOf(ReferenciaIndisponivelException.class).hasMessageNotContaining("chave-teste");
        servidor.corpo="{\"status\":200,\"extremes\":[]}";
        assertThatThrownBy(()->tides(true,true,"chave").consultar(BigDecimal.ZERO,BigDecimal.ZERO,LocalDate.now(),1)).isInstanceOf(ReferenciaIndisponivelException.class);
    }
    @ParameterizedTest @ValueSource(strings={"[]","[{}]"}) void municipiosVaziosOuIncompletosNaoGanhamDadosFabricados(String corpo){
        servidor.corpo=corpo;var r=new IbgeClient(http,servidor.url()).municipios("SC");
        if(r.isEmpty()) assertThat(r).isEmpty(); else {assertThat(r.getFirst().nome()).isNull();assertThat(r.getFirst().siglaUf()).isNull();}
    }
    @ParameterizedTest @ValueSource(strings={"nao-json","{\"error\":\"falhou\"}","[42]"}) void rejeitaRespostasInvalidas(String corpo){
        servidor.corpo=corpo;assertThatThrownBy(()->new IbgeClient(http,servidor.url()).paises()).isInstanceOf(ReferenciaIndisponivelException.class);
    }
    @Test void timeoutEHttp503NaoVazamUrlNemChave(){
        servidor.status=503;
        assertThatThrownBy(()->http.consultar(servidor.url(),"/api/v3",Map.of("key","segredo"))).isInstanceOf(ReferenciaIndisponivelException.class).hasMessageNotContaining("segredo").hasMessageNotContaining(servidor.url());
        servidor.status=200;servidor.atraso=750;
        assertThatThrownBy(()->http.consultar(servidor.url(),"/api/v3",Map.of())).isInstanceOf(ReferenciaIndisponivelException.class);
    }
    @Test void WorldTidesAceitaDataSemEpochOuSemDoisPontosNoFusoESinalizaCamposAusentes(){
        servidor.corpo="""
                {"status":200,"copyright":"TESTE","responseDatum":"MSL","extremes":[
                 {"date":"2026-10-22T00:00:00+0000","type":"High","height":null}]}
                """;
        var r=tides(true,true,"chave-teste").consultar(BigDecimal.ZERO,BigDecimal.ZERO,LocalDate.now(),1);
        assertThat(r.incompleta()).isTrue();assertThat(r.registros().getFirst().altura()).isNull();
        assertThat(r.registros().getFirst().dataHora()).isEqualTo(Instant.parse("2026-10-22T00:00:00Z"));
        servidor.corpo="{\"status\":200,\"copyright\":\"TESTE\",\"responseDatum\":\"MSL\",\"extremes\":[]}";
        assertThat(tides(true,true,"chave-teste").consultar(BigDecimal.ZERO,BigDecimal.ZERO,LocalDate.now(),1).registros()).isEmpty();
    }
    private WorldTidesClient tides(boolean enabled,boolean autorizado,String chave){return new WorldTidesClient(http,servidor.url(),chave,enabled,autorizado);}
}
