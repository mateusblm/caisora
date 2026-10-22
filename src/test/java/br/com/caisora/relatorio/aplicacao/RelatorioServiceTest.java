package br.com.caisora.relatorio.aplicacao;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.caisora.relatorio.dominio.DadosRelatorio;
import br.com.caisora.relatorio.dominio.DadosRelatorio.VagaOcupada;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.movimentacao.dominio.*;
import br.com.caisora.contrato.dominio.*;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class RelatorioServiceTest {
 private final DadosRelatorio dados=mock(DadosRelatorio.class);
 private final RelatorioService service=new RelatorioService(dados);
 private final UUID org=UUID.randomUUID();
 private final LocalDate inicio=LocalDate.of(2026,10,1),fim=LocalDate.of(2026,10,2);
 @Test void calculaOcupacaoEAgrupamentosComFiltrosDaOrganizacao() {
  when(dados.vagas(org,"Norte",TipoVaga.MOLHADA)).thenReturn(List.of(
   new VagaOcupada("Norte",TipoVaga.MOLHADA,true),new VagaOcupada("Norte",TipoVaga.MOLHADA,false),new VagaOcupada("Norte",TipoVaga.MOLHADA,true)));
  var r=service.ocupacao(org," Norte ",TipoVaga.MOLHADA);
  assertThat(r.totalVagasAtivas()).isEqualTo(3);assertThat(r.ocupadas()).isEqualTo(2);assertThat(r.disponiveis()).isEqualTo(1);
  assertThat(r.taxaOcupacao()).isEqualByComparingTo("66.67");assertThat(r.grupos()).hasSize(1);
  assertThat(r.grupos().getFirst().taxaOcupacao()).isEqualByComparingTo("66.67");
  verify(dados).vagas(org,"Norte",TipoVaga.MOLHADA);verifyNoMoreInteractions(dados);
 }
 @Test void ocupacaoVaziaTemTaxaZero() {
  when(dados.vagas(org,null,null)).thenReturn(List.of());var r=service.ocupacao(org,"  ",null);
  assertThat(r.taxaOcupacao()).isZero();assertThat(r.disponiveis()).isZero();assertThat(r.grupos()).isEmpty();
 }
 @Test void gruposPreservamSetoresNulosETiposDiferentes() {
  when(dados.vagas(org,null,null)).thenReturn(List.of(new VagaOcupada(null,TipoVaga.SECA,true),new VagaOcupada("A",TipoVaga.POITA,false)));
  var r=service.ocupacao(org,null,null);assertThat(r.grupos()).hasSize(2);assertThat(r.taxaOcupacao()).isEqualByComparingTo("50");
 }
 @Test void calculaDuracaoSoComDadosValidosEPontualidadeNoLimite() {
  var agenda=Instant.parse("2026-10-01T10:00:00Z");
  var pontual=mov(StatusMovimentacao.CONCLUIDA,agenda.minusSeconds(120),agenda,agenda);
  var atrasada=mov(StatusMovimentacao.CONCLUIDA,agenda,agenda.plusSeconds(60),agenda);
  var semInicio=mov(StatusMovimentacao.CONCLUIDA,null,agenda,agenda);
  var invertida=mov(StatusMovimentacao.CONCLUIDA,agenda.plusSeconds(10),agenda,agenda);
  var semConclusao=mov(StatusMovimentacao.CONCLUIDA,agenda,null,agenda);
  var cancelada=mov(StatusMovimentacao.CANCELADA,null,null,agenda);
  when(dados.movimentacoes(org,inicio.atStartOfDay(ZoneOffset.UTC).toInstant(),fim.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),TipoMovimentacao.RETIRADA))
   .thenReturn(List.of(pontual,atrasada,semInicio,invertida,semConclusao,cancelada));
  var r=service.movimentacoes(org,inicio,fim,TipoMovimentacao.RETIRADA);
  assertThat(r.total()).isEqualTo(6);assertThat(r.concluidas()).isEqualTo(5);assertThat(r.canceladas()).isEqualTo(1);
  assertThat(r.amostrasDuracao()).isEqualTo(2);assertThat(r.tempoMedioExecucaoSegundos()).isEqualByComparingTo("90");
  assertThat(r.concluidasNoPrazo()).isEqualTo(3);assertThat(r.percentualNoPrazo()).isEqualByComparingTo("60");
 }
 @Test void movimentacoesVaziasNaoInventamDuracao() {
  when(dados.movimentacoes(any(),any(),any(),any())).thenReturn(List.of());var r=service.movimentacoes(org,inicio,fim,null);
  assertThat(r.tempoMedioExecucaoSegundos()).isNull();assertThat(r.percentualNoPrazo()).isZero();assertThat(r.porSituacao().values()).allMatch(n->n==0);
 }
 @Test void rejeitaPeriodosInvertidosObrigatoriosESetorExtensoAntesDaPesquisa() {
  assertThatThrownBy(()->service.movimentacoes(org,fim,inicio,null)).isInstanceOf(DadosInvalidosException.class);
  assertThatThrownBy(()->service.movimentacoes(org,null,fim,null)).isInstanceOf(DadosInvalidosException.class);
  assertThatThrownBy(()->service.contratos(org,null,null,fim,inicio)).isInstanceOf(DadosInvalidosException.class);
  assertThatThrownBy(()->service.ocupacao(org,"x".repeat(101),null)).isInstanceOf(DadosInvalidosException.class);verifyNoInteractions(dados);
 }
 @Test void carteiraSeparaPeriodicidadesECalculaPercentuais() {
  var lista=List.of(contrato(StatusContrato.ATIVO,PeriodicidadeContrato.MENSAL,"100"),contrato(StatusContrato.ATIVO,PeriodicidadeContrato.MENSAL,"50"),contrato(StatusContrato.CANCELADO,PeriodicidadeContrato.ANUAL,"1200"));
  when(dados.contratos(org,null,null,inicio,fim)).thenReturn(lista);
  var r=service.contratos(org,null,null,inicio,fim);
  assertThat(r.total()).isEqualTo(3);assertThat(r.percentualPorSituacao().get(StatusContrato.ATIVO)).isEqualByComparingTo("66.67");
  assertThat(r.valoresBasePorPeriodicidade().get(PeriodicidadeContrato.MENSAL)).isEqualByComparingTo("150");
  assertThat(r.valoresBasePorPeriodicidade().get(PeriodicidadeContrato.ANUAL)).isEqualByComparingTo("1200");
  assertThat(r.porPeriodicidade().get(PeriodicidadeContrato.MENSAL)).isEqualTo(2);
 }
 @Test void carteiraVaziaEFiltradaTemPercentuaisZero() {
  when(dados.contratos(org,StatusContrato.ATIVO,PeriodicidadeContrato.MENSAL,null,null)).thenReturn(List.of());
  var r=service.contratos(org,StatusContrato.ATIVO,PeriodicidadeContrato.MENSAL,null,null);
  assertThat(r.total()).isZero();assertThat(r.percentualPorSituacao().values()).allMatch(n->n.signum()==0);
  verify(dados).contratos(org,StatusContrato.ATIVO,PeriodicidadeContrato.MENSAL,null,null);
 }
 private Movimentacao mov(StatusMovimentacao status,Instant inicio,Instant fim,Instant agenda) {
  var m=mock(Movimentacao.class);when(m.getStatus()).thenReturn(status);
  if(status==StatusMovimentacao.CONCLUIDA){when(m.getIniciadaEm()).thenReturn(inicio);when(m.getConcluidaEm()).thenReturn(fim);when(m.getAgendadaPara()).thenReturn(agenda);}return m;
 }
 private Contrato contrato(StatusContrato status,PeriodicidadeContrato periodicidade,String valor) {
  var c=mock(Contrato.class);when(c.getStatus()).thenReturn(status);when(c.getPeriodicidade()).thenReturn(periodicidade);when(c.getValorBase()).thenReturn(new BigDecimal(valor));return c;
 }
}
