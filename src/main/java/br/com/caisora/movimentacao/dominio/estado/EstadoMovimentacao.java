package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** State: o estado atual define edição e transições da ordem de movimentação. */
public interface EstadoMovimentacao {
    default void editar() {
        throw new IllegalStateException("A movimentacao precisa estar agendada");
    }
    default StatusMovimentacao iniciar() {
        throw new IllegalStateException("A movimentacao precisa estar agendada");
    }
    default StatusMovimentacao cancelar() {
        throw new IllegalStateException("A movimentacao precisa estar agendada");
    }
    default StatusMovimentacao concluir() {
        throw new IllegalStateException("A movimentacao precisa estar em execucao");
    }
}
