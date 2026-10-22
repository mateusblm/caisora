package br.com.caisora.relatorio.infraestrutura;
import static org.assertj.core.api.Assertions.*;
import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.cliente.dominio.*;
import br.com.caisora.embarcacao.dominio.*;
import br.com.caisora.usuario.dominio.*;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.vaga.dominio.*;
import br.com.caisora.contrato.dominio.*;
import br.com.caisora.movimentacao.dominio.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
@DataJpaTest
@Import({TestcontainersConfiguration.class,DadosRelatorioJpa.class})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker=true)
class DadosRelatorioJpaTest {
 @Autowired EntityManager em;
 @Autowired DadosRelatorioJpa dados;
 private Organizacao a,b;
 @BeforeEach void configurar(){a=org("a");b=org("b");}
 @Test void ocupacaoFiltraAtivasSetorTipoEOrganizacao(){
  var ocupada=vaga(a,"01",TipoVaga.SECA,"Norte");vaga(a,"02",TipoVaga.SECA,"Norte");vaga(a,"03",TipoVaga.MOLHADA,"Sul");
  var inativa=vaga(a,"04",TipoVaga.SECA,"Norte");inativa.inativar();
  vaga(b,"01",TipoVaga.SECA,"Norte");
  var barco=barco(a);em.persist(new Ocupacao(a,barco,ocupada,Instant.now(),null,null));em.flush();em.clear();
  var lista=dados.vagas(a.getId(),"norte",TipoVaga.SECA);
  assertThat(lista).hasSize(2);assertThat(lista).filteredOn(v->v.ocupada()).hasSize(1);
  assertThat(dados.vagas(a.getId(),null,null)).hasSize(3);
  assertThat(dados.vagas(b.getId(),null,null)).hasSize(1);
 }
 @Test void contratosCombinamFiltrosEIsolamOrganizacoes(){
  contrato(a,"01",PeriodicidadeContrato.MENSAL,LocalDate.of(2026,10,1));
  contrato(a,"02",PeriodicidadeContrato.ANUAL,LocalDate.of(2026,9,1));contrato(b,"01",PeriodicidadeContrato.MENSAL,LocalDate.of(2026,10,1));
  em.flush();em.clear();
  assertThat(dados.contratos(a.getId(),StatusContrato.RASCUNHO,PeriodicidadeContrato.MENSAL,LocalDate.of(2026,10,1),LocalDate.of(2026,10,1))).hasSize(1);
  assertThat(dados.contratos(a.getId(),null,null,null,null)).hasSize(2);
  assertThat(dados.contratos(a.getId(),StatusContrato.ATIVO,null,null,null)).isEmpty();
 }
 @Test void movimentacoesUsamAgendamentoIntervaloSemiabertoEOrganizacao(){
  Instant inicio=Instant.parse("2026-10-01T00:00:00Z"),fim=Instant.parse("2026-10-02T00:00:00Z");
  movimento(a,inicio);movimento(a,fim.minusSeconds(1));movimento(a,fim);movimento(b,inicio);em.flush();em.clear();
  assertThat(dados.movimentacoes(a.getId(),inicio,fim,null)).hasSize(2);
  assertThat(dados.movimentacoes(a.getId(),inicio,fim,TipoMovimentacao.TRANSFERENCIA)).isEmpty();
 }
 @Test void migrationPersisteCoordenadasEConsultaNaoMudaAuditoria(){
  a.configurarCoordenadas(new BigDecimal("-25.123456"),new BigDecimal("-48.123456"));em.flush();em.clear();
  var org=em.find(Organizacao.class,a.getId());var atualizacao=org.getAtualizadaEm();dados.vagas(a.getId(),null,null);em.flush();em.clear();
  var lida=em.find(Organizacao.class,a.getId());assertThat(lida.getLatitude()).isEqualByComparingTo("-25.123456");assertThat(lida.getAtualizadaEm()).isEqualTo(atualizacao);
 }
 private Organizacao org(String slug){var o=Organizacao.criar("Marina "+slug,slug,null,null,slug+"@example.com",null);em.persist(o);return o;}
 private Vaga vaga(Organizacao org,String codigo,TipoVaga tipo,String setor){var v=new Vaga(org,codigo,tipo,setor,null,BigDecimal.TEN,BigDecimal.TEN,null,null,null,false,false,null);em.persist(v);return v;}
 private Embarcacao barco(Organizacao org){var c=new Cliente(org,TipoPessoa.FISICA,"Cliente",null,String.format("%014d",java.util.concurrent.ThreadLocalRandom.current().nextLong(1,99999999999999L)),null,null,null,null);em.persist(c);
 var e=Embarcacao.builder().organizacao(org).proprietario(c).nome("Barco").tipo(TipoEmbarcacao.LANCHA).codigoPaisBandeira("BR").numeroInscricao(java.util.UUID.randomUUID().toString()).comprimentoTotalMetros(BigDecimal.TEN).bocaMetros(BigDecimal.ONE).tipoPropulsao(TipoPropulsao.MOTOR).construir();em.persist(e);return e;}
 private Usuario usuario(Organizacao org){var u=Usuario.criar(org,"Administrador",java.util.UUID.randomUUID()+"@example.com","senha",PerfilUsuario.ADMINISTRADOR_MARINA);em.persist(u);return u;}
 private void contrato(Organizacao org,String numero,PeriodicidadeContrato periodicidade,LocalDate inicio){var barco=barco(org);
 em.persist(Contrato.builder().organizacao(org).numero(numero).cliente(barco.getProprietario()).embarcacao(barco).tipoVagaContratada(TipoVaga.SECA).periodicidade(periodicidade).dataInicio(inicio).valorBase(BigDecimal.TEN).diaVencimento(10).criadoPor(usuario(org)).construir());}
 private void movimento(Organizacao org,Instant agenda){var barco=barco(org);em.persist(new Movimentacao(org,barco,TipoMovimentacao.DESLOCAMENTO_INTERNO,PrioridadeMovimentacao.NORMAL,TipoPosicaoEmbarcacao.AREA_SERVICO,null,null,TipoPosicaoEmbarcacao.AREA_SERVICO,null,null,agenda,usuario(org),null,null));}
}
