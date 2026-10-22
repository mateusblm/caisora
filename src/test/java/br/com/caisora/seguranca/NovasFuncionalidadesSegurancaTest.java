package br.com.caisora.seguranca;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.organizacao.api.*;
import br.com.caisora.relatorio.api.RelatorioController;
import br.com.caisora.relatorio.aplicacao.RelatorioService;
import br.com.caisora.ambiente.api.AmbienteController;
import br.com.caisora.ambiente.aplicacao.AmbienteService;
import br.com.caisora.variabilidade.api.*;
import br.com.caisora.variabilidade.aplicacao.*;
import br.com.caisora.variabilidade.infraestrutura.*;
import br.com.caisora.variabilidade.dominio.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;

class NovasFuncionalidadesSegurancaTest {
 private AnnotationConfigApplicationContext context;
 private final UUID org=UUID.randomUUID();
 @BeforeEach void iniciar(){context=new AnnotationConfigApplicationContext(Config.class);}
 @AfterEach void fechar(){SecurityContextHolder.clearContext();context.close();}
 @Test void marinaNaoAlteraModulosPorNenhumEndpointNemPeloService(){
  autenticar("ADMINISTRADOR_MARINA");
  assertThatThrownBy(()->context.getBean(VariabilidadeController.class).atualizarModulo(ModuloSistema.CONTRATOS,new AtualizarModuloRequest(true))).isInstanceOf(AccessDeniedException.class);
  assertThatThrownBy(()->context.getBean(ModulosOrganizacaoController.class).atualizar(UUID.randomUUID(),ModuloSistema.CONTRATOS,new AtualizarModuloRequest(true))).isInstanceOf(AccessDeniedException.class);
  assertThatThrownBy(()->context.getBean(VariabilidadeService.class).atualizarModulo(org,ModuloSistema.CONTRATOS,true)).isInstanceOf(AccessDeniedException.class);
  verifyNoInteractions(context.getBean(ModuloOrganizacaoRepositoryJpa.class));
 }
 @Test void plataformaPodeDefinirModulosDeUmaOrganizacaoExplicita(){
  autenticar("ADMINISTRADOR_PLATAFORMA");UUID alvo=UUID.randomUUID();
  context.getBean(ModulosOrganizacaoController.class).atualizar(alvo,ModuloSistema.CHECKLIST_SAIDA,new AtualizarModuloRequest(true));
  verify(context.getBean(OrganizacaoService.class)).buscarPorId(alvo);
  verify(context.getBean(ModuloOrganizacaoRepositoryJpa.class)).save(argThat(m->m.getOrganizacaoId().equals(alvo)&&m.getModulo()==ModuloSistema.CHECKLIST_SAIDA&&m.isAtivo()));
 }
 @Test void marinaMantemPermissoesInternasSemLiberarModulo(){
  autenticar("ADMINISTRADOR_MARINA");
  context.getBean(VariabilidadeController.class).atualizarPermissoes(br.com.caisora.usuario.dominio.PerfilUsuario.ATENDENTE,
   new AtualizarPermissoesPerfilRequest(Set.of("RELATORIOS:VISUALIZAR")));
  assertThat(context.getBean(VariabilidadeService.class).permitido(org,br.com.caisora.usuario.dominio.PerfilUsuario.ATENDENTE,ModuloSistema.RELATORIOS,AcaoSistema.VISUALIZAR)).isFalse();
  verify(context.getBean(PermissaoPerfilOrganizacaoRepositoryJpa.class)).flush();
 }
 @Test void coordenadasSempreUsamOrganizacaoDoTokenEAtendenteNaoAltera(){
  autenticar("ADMINISTRADOR_MARINA");var c=context.getBean(CoordenadasController.class);
  var request=new CoordenadasRequest(BigDecimal.ZERO,BigDecimal.TEN);c.configurar(request);
  verify(context.getBean(OrganizacaoService.class)).configurarCoordenadas(org,request);
  autenticar("ATENDENTE");assertThatThrownBy(()->c.configurar(request)).isInstanceOf(AccessDeniedException.class);
 }
 @Test void relatoriosExigemModuloLiberadoEPermissaoDoPerfil(){
  autenticar("GERENTE");var controller=context.getBean(RelatorioController.class);
  assertThatThrownBy(()->controller.ocupacao(null,null)).isInstanceOf(AccessDeniedException.class);
  liberar(ModuloSistema.RELATORIOS);controller.ocupacao(null,null);
  verify(context.getBean(RelatorioService.class)).ocupacao(org,null,null);
  autenticar("ATENDENTE");assertThatThrownBy(()->controller.ocupacao(null,null)).isInstanceOf(AccessDeniedException.class);
 }
 @Test void relatoriosNaoContornamBloqueioDoModuloDeNegocio(){
  autenticar("GERENTE");liberar(ModuloSistema.RELATORIOS);
  var registro=ModuloOrganizacao.criar(org,ModuloSistema.CONTRATOS,false);
  when(context.getBean(ModuloOrganizacaoRepositoryJpa.class).findByOrganizacaoIdAndModulo(org,ModuloSistema.CONTRATOS)).thenReturn(Optional.of(registro));
  assertThatThrownBy(()->context.getBean(RelatorioController.class).contratos(null,null,null,null)).isInstanceOf(AccessDeniedException.class);
  verifyNoInteractions(context.getBean(RelatorioService.class));
 }
 @Test void ambienteUsaOrganizacaoAutenticadaSomenteQuandoLiberado(){
  autenticar("ATENDENTE");var c=context.getBean(AmbienteController.class);var hoje=LocalDate.now();
  assertThatThrownBy(()->c.maritimo(hoje,hoje)).isInstanceOf(AccessDeniedException.class);
  liberar(ModuloSistema.AMBIENTE);c.maritimo(hoje,hoje);verify(context.getBean(AmbienteService.class)).consultar(org,true,hoje,hoje);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.EnumSource(value=ModuloSistema.class, names={"RELATORIOS","VAGAS","OCUPACOES"})
 void permanenciaHistoricaExigeOsTresModulos(ModuloSistema bloqueado){
  autenticar("GERENTE");liberar(ModuloSistema.RELATORIOS);liberar(ModuloSistema.VAGAS);liberar(ModuloSistema.OCUPACOES);
  when(context.getBean(ModuloOrganizacaoRepositoryJpa.class).findByOrganizacaoIdAndModulo(org,bloqueado))
   .thenReturn(Optional.of(ModuloOrganizacao.criar(org,bloqueado,false)));
  var dia=LocalDate.of(2026,10,10);
  assertThatThrownBy(()->context.getBean(RelatorioController.class).permanenciaVagas(dia,dia,null,null)).isInstanceOf(AccessDeniedException.class);
  verifyNoInteractions(context.getBean(RelatorioService.class));
 }
 @Test void permanenciaUsaTenantDoTokenENegaPerfilSemPermissao(){
  autenticar("GERENTE");liberar(ModuloSistema.RELATORIOS);
  var dia=LocalDate.of(2026,10,10);
  context.getBean(RelatorioController.class).permanenciaVagas(dia,dia,br.com.caisora.vaga.dominio.TipoVaga.SECA,"Norte");
  verify(context.getBean(RelatorioService.class)).permanenciaVagas(org,dia,dia,"Norte",br.com.caisora.vaga.dominio.TipoVaga.SECA);
  autenticar("ATENDENTE");
  assertThatThrownBy(()->context.getBean(RelatorioController.class).permanenciaVagas(dia,dia,null,null)).isInstanceOf(AccessDeniedException.class);
 }
 private void liberar(ModuloSistema modulo){when(context.getBean(ModuloOrganizacaoRepositoryJpa.class).findByOrganizacaoIdAndModulo(org,modulo)).thenReturn(Optional.of(ModuloOrganizacao.criar(org,modulo,true)));}
 private void autenticar(String perfil){var jwt=Jwt.withTokenValue("teste").header("alg","HS256").subject(UUID.randomUUID().toString())
 .claim("perfil",perfil).claim("organizacaoId",org.toString()).build();
 SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+perfil))));}
 @Configuration @EnableMethodSecurity static class Config {
  @Bean ModuloOrganizacaoRepositoryJpa modulos(){return mock(ModuloOrganizacaoRepositoryJpa.class);}
  @Bean PermissaoPerfilOrganizacaoRepositoryJpa permissoes(){return mock(PermissaoPerfilOrganizacaoRepositoryJpa.class);}
  @Bean VariabilidadeService variabilidade(ModuloOrganizacaoRepositoryJpa m,PermissaoPerfilOrganizacaoRepositoryJpa p){return new VariabilidadeService(m,p,new PoliticaPadraoVariabilidade());}
  @Bean(name="acesso") ControleAcesso acesso(VariabilidadeService s){return new ControleAcesso(s);}
  @Bean LeitorTokenJwt leitor(){return new LeitorTokenJwt();}
  @Bean OrganizacaoService organizacoes(){return mock(OrganizacaoService.class);}
  @Bean RelatorioService relatorios(){return mock(RelatorioService.class);}
  @Bean AmbienteService ambiente(){return mock(AmbienteService.class);}
  @Bean VariabilidadeController variabilidadeController(VariabilidadeService s,LeitorTokenJwt l){return new VariabilidadeController(s,l);}
  @Bean ModulosOrganizacaoController modulosController(VariabilidadeService s,OrganizacaoService o){return new ModulosOrganizacaoController(s,o);}
  @Bean CoordenadasController coordenadasController(OrganizacaoService o,LeitorTokenJwt l){return new CoordenadasController(o,l);}
  @Bean RelatorioController relatorioController(RelatorioService s,LeitorTokenJwt l){return new RelatorioController(s,l);}
  @Bean AmbienteController ambienteController(AmbienteService s,LeitorTokenJwt l){return new AmbienteController(s,l);}
 }
}
