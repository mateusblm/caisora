package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** Estado concreto: operações não sobrescritas são recusadas pela interface. */
public class MovimentacaoEmExecucao implements EstadoMovimentacao {

    @Override
    public StatusMovimentacao concluir() { return StatusMovimentacao.CONCLUIDA; }
}
