package br.com.caisora.movimentacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.Vaga;

class MovimentacaoTest {

    private Organizacao organizacao;
    private Embarcacao embarcacao;
    private Usuario solicitante;
    private Usuario operador;
    private Vaga vagaOrigem;
    private Vaga vagaDestino;

    @BeforeEach
    void configurar() {
        organizacao = mock(Organizacao.class);
        embarcacao = mock(Embarcacao.class);
        solicitante = mock(Usuario.class);
        operador = mock(Usuario.class);
        vagaOrigem = mock(Vaga.class);
        vagaDestino = mock(Vaga.class);

        when(vagaOrigem.getId()).thenReturn(UUID.randomUUID());
        when(vagaDestino.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void deveCriarLancamentoAgendado() {
        Instant agendadaPara = Instant.now().plusSeconds(3600);

        Movimentacao movimentacao = new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.LANCAMENTO,
                PrioridadeMovimentacao.ALTA,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                "  Canal principal  ",
                agendadaPara,
                solicitante,
                operador,
                "  Cliente aguardando no píer  ");

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.AGENDADA);
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacao.LANCAMENTO);
        assertThat(movimentacao.getPrioridade()).isEqualTo(PrioridadeMovimentacao.ALTA);
        assertThat(movimentacao.getTipoPosicaoOrigem()).isEqualTo(TipoPosicaoEmbarcacao.VAGA);
        assertThat(movimentacao.getVagaOrigem()).isSameAs(vagaOrigem);
        assertThat(movimentacao.getTipoPosicaoDestino()).isEqualTo(TipoPosicaoEmbarcacao.AGUA);
        assertThat(movimentacao.getVagaDestino()).isNull();
        assertThat(movimentacao.getDescricaoDestino()).isEqualTo("Canal principal");
        assertThat(movimentacao.getObservacoes()).isEqualTo("Cliente aguardando no píer");
        assertThat(movimentacao.getAgendadaPara()).isEqualTo(agendadaPara);
        assertThat(movimentacao.getVersao()).isNull();
    }

    @Test
    void deveExecutarFluxoAgendadaEmExecucaoConcluida() {
        Movimentacao movimentacao = criarLancamento();
        Instant iniciadaEm = movimentacao.getCriadaEm();
        Instant concluidaEm = iniciadaEm.plusSeconds(60);

        movimentacao.iniciar(operador, iniciadaEm);

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.EM_EXECUCAO);
        assertThat(movimentacao.getOperadorResponsavel()).isSameAs(operador);
        assertThat(movimentacao.getIniciadaEm()).isEqualTo(iniciadaEm);

        movimentacao.concluir(operador, concluidaEm);

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.CONCLUIDA);
        assertThat(movimentacao.getConcluidaEm()).isEqualTo(concluidaEm);
        assertThat(movimentacao.getCanceladaEm()).isNull();
    }

    @Test
    void naoDeveConcluirMovimentacaoQueNaoFoiIniciada() {
        Movimentacao movimentacao = criarLancamento();

        assertThatThrownBy(() -> movimentacao.concluir(operador, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("execucao");
    }

    @Test
    void deveCancelarMovimentacaoAgendada() {
        Movimentacao movimentacao = criarLancamento();
        Instant canceladaEm = Instant.now();

        movimentacao.cancelar(canceladaEm, "Condições climáticas desfavoráveis");

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.CANCELADA);
        assertThat(movimentacao.getCanceladaEm()).isEqualTo(canceladaEm);
        assertThat(movimentacao.getMotivoCancelamento())
                .isEqualTo("Condições climáticas desfavoráveis");
    }

    @Test
    void naoDeveCancelarMovimentacaoEmExecucao() {
        Movimentacao movimentacao = criarLancamento();
        movimentacao.iniciar(operador, movimentacao.getCriadaEm());

        assertThatThrownBy(() -> movimentacao.cancelar(Instant.now(), "Cancelamento indevido"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agendada");
    }

    @Test
    void naoDeveCriarTransferenciaParaAMesmaVaga() {
        assertThatThrownBy(() -> new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.TRANSFERENCIA,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                Instant.now().plusSeconds(3600),
                solicitante,
                operador,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferentes");
    }

    @Test
    void deveCriarRetornoParaVagaSaindoDaAreaDeServico() {
        Movimentacao movimentacao = new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.RETORNO_PARA_VAGA,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.AREA_SERVICO,
                null,
                "Box de manutencao",
                TipoPosicaoEmbarcacao.VAGA,
                vagaDestino,
                null,
                Instant.now().plusSeconds(3600),
                solicitante,
                operador,
                null);

        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacao.RETORNO_PARA_VAGA);
        assertThat(movimentacao.getTipoPosicaoOrigem()).isEqualTo(TipoPosicaoEmbarcacao.AREA_SERVICO);
        assertThat(movimentacao.getTipoPosicaoDestino()).isEqualTo(TipoPosicaoEmbarcacao.VAGA);
        assertThat(movimentacao.getVagaDestino()).isSameAs(vagaDestino);
    }

    @Test
    void naoDeveCriarRetiradaSaindoDaAreaDeServico() {
        assertThatThrownBy(() -> new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.RETIRADA,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.AREA_SERVICO,
                null,
                "Box de manutencao",
                TipoPosicaoEmbarcacao.VAGA,
                vagaDestino,
                null,
                Instant.now().plusSeconds(3600),
                solicitante,
                operador,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Retirada");
    }

    @Test
    void naoDeveAtualizarMovimentacaoConcluida() {
        Movimentacao movimentacao = criarLancamento();
        movimentacao.iniciar(operador, movimentacao.getCriadaEm());
        movimentacao.concluir(operador, movimentacao.getIniciadaEm().plusSeconds(60));

        assertThatThrownBy(() -> movimentacao.atualizarDados(
                PrioridadeMovimentacao.URGENTE,
                TipoPosicaoEmbarcacao.PIER_ESPERA,
                null,
                "Píer norte",
                Instant.now().plusSeconds(7200),
                operador,
                "Reagendamento"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agendada");
    }

    private Movimentacao criarLancamento() {
        return new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.LANCAMENTO,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                "Canal principal",
                Instant.now().plusSeconds(3600),
                solicitante,
                null,
                null);
    }
}
