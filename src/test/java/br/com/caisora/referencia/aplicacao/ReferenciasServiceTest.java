package br.com.caisora.referencia.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.caisora.referencia.api.ReferenciasResponse.*;
import br.com.caisora.referencia.infraestrutura.ibge.IbgeClient;
import br.com.caisora.referencia.infraestrutura.brasilapi.BrasilApiClient;
import br.com.caisora.referencia.infraestrutura.worldtides.WorldTidesClient;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.organizacao.api.CoordenadasResponse;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReferenciasServiceTest {
    private final IbgeClient ibge=mock(IbgeClient.class);
    private final BrasilApiClient brasil=mock(BrasilApiClient.class);
    private final WorldTidesClient tides=mock(WorldTidesClient.class);
    private final OrganizacaoService orgs=mock(OrganizacaoService.class);
    private final ReferenciasService service=new ReferenciasService(ibge,brasil,tides,orgs);
    private final UUID org=UUID.randomUUID();
    private final LocalDate hoje=LocalDate.now(ZoneOffset.UTC);

    @Test void paisesPermitemBuscaPorNomeOuCodigoSemPersistencia(){
        when(ibge.paises()).thenReturn(List.of(new Pais(76,"BR","BRA","Brasil"),new Pais(32,"AR","ARG","Argentina")));
        assertThat(service.paises(" bra ").registros()).hasSize(1);
        assertThat(service.paises("76").registros().getFirst().nome()).isEqualTo("Brasil");
        assertThat(service.paises("sem-correspondencia").registros()).isEmpty();verifyNoInteractions(orgs,brasil,tides);
    }
    @Test void municipiosValidamENormalizamUfEBuscamCodigo(){
        when(ibge.municipios("SC")).thenReturn(List.of(new Municipio(4205407,"Florianópolis",42,"SC","Santa Catarina")));
        assertThat(service.municipios(" sc ","4205407").registros()).hasSize(1);
        verify(ibge).municipios("SC");
    }
    @Test void camposAusentesSinalizamRespostaParcialSemPreencherCadastro(){
        when(ibge.paises()).thenReturn(List.of(new Pais(null,"BR",null,"Brasil")));
        assertThat(service.paises(null).incompleta()).isTrue();
        when(ibge.municipios("SC")).thenReturn(List.of(new Municipio(4205407,"Florianópolis",null,null,null)));
        var r=service.municipios("SC",null);assertThat(r.incompleta()).isTrue();assertThat(r.registros().getFirst().nomeUf()).isNull();
    }
    @Test void feriadosMantemClassificacaoDoProvedorEEncaminhamUfOpcional(){
        when(brasil.feriados(2026,"SC")).thenReturn(List.of(new Feriado(LocalDate.of(2026,8,11),"Nome fornecido","state","SC",null)));
        var r=service.feriados(2026,"sc");assertThat(r.incompleta()).isFalse();assertThat(r.registros().getFirst().tipo()).isEqualTo("state");
        when(brasil.feriados(1900,null)).thenReturn(List.of());when(brasil.feriados(2199,null)).thenReturn(List.of());
        assertThat(service.feriados(1900,"").registros()).isEmpty();assertThat(service.feriados(2199,null).registros()).isEmpty();
    }
    @Test void filtrosInvalidosNaoChamamProvedores(){
        assertThatThrownBy(()->service.paises("x".repeat(101))).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.municipios("ZZ",null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.municipios(null,null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.feriados(1899,null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.feriados(2200,"SC")).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.feriados(2026,"../SC")).isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(ibge,brasil,tides,orgs);
    }
    @Test void maresDesabilitadasNaoBuscamCoordenadasNemPrevisoes(){
        assertThat(service.disponibilidadeMares().habilitada()).isFalse();
        assertThatThrownBy(()->service.mares(org,hoje,1)).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("desabilitada");
        verifyNoInteractions(orgs);verify(tides,never()).consultar(any(),any(),any(),anyInt());
    }
    @Test void maresUsamApenasCoordenadasDaOrganizacaoAutenticada(){
        when(tides.disponivel()).thenReturn(true);when(orgs.coordenadas(org)).thenReturn(new CoordenadasResponse(new BigDecimal("-27.59"),new BigDecimal("-48.55")));
        service.mares(org,hoje,1);
        verify(tides).consultar(new BigDecimal("-27.59"),new BigDecimal("-48.55"),hoje,1);verify(orgs).coordenadas(org);
    }
    @Test void maresExigemCoordenadasEValidamJanelaEDuracao(){
        assertThatThrownBy(()->service.mares(org,hoje.minusDays(1),1)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.mares(org,hoje,8)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.mares(org,hoje,0)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.mares(org,hoje.plusDays(6),2)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(()->service.mares(org,null,1)).isInstanceOf(DadosInvalidosException.class);
        when(tides.disponivel()).thenReturn(true);when(orgs.coordenadas(org)).thenReturn(new CoordenadasResponse(null,null));
        assertThatThrownBy(()->service.mares(org,hoje,1)).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("coordenadas");
        verify(tides,never()).consultar(any(),any(),any(),anyInt());
    }
    @Test void problemasDeInterpretacaoViraramErroSeguroSemConteudoDoProvedor(){
        when(brasil.feriados(2026,null)).thenThrow(new IllegalArgumentException("URL e segredo do provedor"));
        assertThatThrownBy(()->service.feriados(2026,null)).isInstanceOf(ReferenciaIndisponivelException.class).hasMessageNotContaining("segredo");
    }
}
