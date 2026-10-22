package br.com.caisora.ambiente.api;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.caisora.ambiente.aplicacao.*;
import br.com.caisora.autenticacao.aplicacao.*;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.organizacao.api.CoordenadasController;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.relatorio.api.RelatorioController;
import br.com.caisora.relatorio.aplicacao.RelatorioService;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
class NovosEndpointsTest {
 private MockMvc mvc;private AmbienteService ambiente;private RelatorioService relatorios;
 @BeforeEach void configurar(){
  ambiente=mock(AmbienteService.class);relatorios=mock(RelatorioService.class);var leitor=mock(LeitorTokenJwt.class);
  when(leitor.obterUsuarioAutenticado()).thenReturn(new UsuarioAutenticado(UUID.randomUUID(),"Admin","admin@example.com",PerfilUsuario.ADMINISTRADOR_MARINA,UUID.randomUUID(),"Marina"));
  var validator=new LocalValidatorFactoryBean();validator.afterPropertiesSet();
  mvc=MockMvcBuilders.standaloneSetup(new AmbienteController(ambiente,leitor),new CoordenadasController(mock(OrganizacaoService.class),leitor),new RelatorioController(relatorios,leitor)).setControllerAdvice(new TratadorGlobalException()).setValidator(validator).build();
 }
 @Test void parametrosAusentesEFormatosInvalidosRetornam400() throws Exception {
  mvc.perform(get("/api/v1/ambiente/meteorologia")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/ambiente/maritimo").param("inicio","invalido").param("fim","2026-10-22")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/relatorios/ocupacao").param("tipo","INEXISTENTE")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/relatorios/movimentacoes")).andExpect(status().isBadRequest());verifyNoInteractions(ambiente,relatorios);
 }
 @Test void permanenciaValidaDatasObrigatoriasEFormatoDosFiltros() throws Exception {
  mvc.perform(get("/api/v1/relatorios/permanencia-vagas")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/relatorios/permanencia-vagas").param("inicio","2026-10-10")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/relatorios/permanencia-vagas").param("inicio","invalido").param("fim","2026-10-10")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/relatorios/permanencia-vagas").param("inicio","2026-10-10").param("fim","2026-10-10").param("tipo","INEXISTENTE")).andExpect(status().isBadRequest());
  verifyNoInteractions(relatorios);
 }
 @Test void coordenadasValidamIntervaloPrecisaoEObrigatoriedade() throws Exception {
  mvc.perform(put("/api/v1/minha-organizacao/coordenadas").contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":91,\"longitude\":-181}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  mvc.perform(put("/api/v1/minha-organizacao/coordenadas").contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":0.1234567}")).andExpect(status().isBadRequest());
 }
 @Test void falhaDoProvedorRetorna503ComMensagemSegura() throws Exception {
  when(ambiente.consultar(any(),eq(false),any(),any())).thenThrow(new ProvedorIndisponivelException());
  mvc.perform(get("/api/v1/ambiente/meteorologia").param("inicio","2026-10-22").param("fim","2026-10-22")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("PROVEDOR_INDISPONIVEL"));
 }
}
