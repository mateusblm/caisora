package br.com.caisora.ambiente.aplicacao;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.caisora.ambiente.infraestrutura.openmeteo.OpenMeteoClient;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.organizacao.api.CoordenadasResponse;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class AmbienteServiceTest {
 private final OrganizacaoService orgs=mock(OrganizacaoService.class);
 private final OpenMeteoClient client=mock(OpenMeteoClient.class);
 private final AmbienteService service=new AmbienteService(orgs,client,true);
 private final UUID org=UUID.randomUUID(); private final LocalDate hoje=LocalDate.now(ZoneOffset.UTC);
 @Test void validaLimitesDoPeriodoAntesDeConsultar() {
  assertThatThrownBy(()->service.consultar(org,false,hoje.minusDays(1),hoje)).isInstanceOf(DadosInvalidosException.class);
  assertThatThrownBy(()->service.consultar(org,true,hoje,hoje.plusDays(16))).isInstanceOf(DadosInvalidosException.class);
  assertThatThrownBy(()->service.consultar(org,false,hoje.plusDays(1),hoje)).isInstanceOf(DadosInvalidosException.class);verifyNoInteractions(orgs,client);
 }
 @Test void exigeCoordenadas() {
  when(orgs.coordenadas(org)).thenReturn(new CoordenadasResponse(null,null));
  assertThatThrownBy(()->service.consultar(org,true,hoje,hoje)).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("coordenadas");verifyNoInteractions(client);
 }
 @Test void usaCoordenadasDaOrganizacaoEPermiteUltimoDia() {
  when(orgs.coordenadas(org)).thenReturn(new CoordenadasResponse(BigDecimal.ZERO,BigDecimal.TEN));
  service.consultar(org,true,hoje,hoje.plusDays(15));verify(client).consultar(true,BigDecimal.ZERO,BigDecimal.TEN,hoje,hoje.plusDays(15));
 }
 @Test void bloqueiaIntegracaoDesabilitada() {
  when(orgs.coordenadas(org)).thenReturn(new CoordenadasResponse(BigDecimal.ZERO,BigDecimal.TEN));
  assertThatThrownBy(()->new AmbienteService(orgs,client,false).consultar(org,false,hoje,hoje)).isInstanceOf(DadosInvalidosException.class);verifyNoInteractions(client);
 }
}
