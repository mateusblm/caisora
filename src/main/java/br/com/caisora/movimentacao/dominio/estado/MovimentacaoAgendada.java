package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** Estado concreto: operações não sobrescritas são recusadas pela interface. */
public class MovimentacaoAgendada implements EstadoMovimentacao {
    @Override
    public void editar() { /* Ordens agendadas permitem edição. */ }

    @Override
    public StatusMovimentacao iniciar() { return StatusMovimentacao.EM_EXECUCAO; }

    @Override
    public StatusMovimentacao cancelar() { return StatusMovimentacao.CANCELADA; }
}
