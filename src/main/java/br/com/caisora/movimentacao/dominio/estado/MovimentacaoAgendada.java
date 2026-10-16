package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** Ordem criada e ainda nao comecou: pode editar, iniciar ou cancelar. Concluir direto nao pode. */
public class MovimentacaoAgendada implements EstadoMovimentacao {
    @Override
    public void editar() { /* Ordens agendadas permitem edição. */ }

    @Override
    public StatusMovimentacao iniciar() { return StatusMovimentacao.EM_EXECUCAO; }

    @Override
    public StatusMovimentacao cancelar() { return StatusMovimentacao.CANCELADA; }
}
