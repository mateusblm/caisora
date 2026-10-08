package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** Seleciona o objeto State correspondente ao status salvo no banco. */
public final class EstadosMovimentacao {
    private EstadosMovimentacao() { }

    public static EstadoMovimentacao para(StatusMovimentacao status) {
        return switch (status) {
            case AGENDADA -> new MovimentacaoAgendada();
            case EM_EXECUCAO -> new MovimentacaoEmExecucao();
            case CONCLUIDA -> new MovimentacaoConcluida();
            case CANCELADA -> new MovimentacaoCancelada();
        };
    }
}
