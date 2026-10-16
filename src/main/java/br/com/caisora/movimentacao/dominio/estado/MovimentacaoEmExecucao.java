package br.com.caisora.movimentacao.dominio.estado;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;

/** Operador ja esta movendo o barco. A partir daqui so concluir; nao da mais pra editar nem cancelar. */
public class MovimentacaoEmExecucao implements EstadoMovimentacao {

    @Override
    public StatusMovimentacao concluir() { return StatusMovimentacao.CONCLUIDA; }
}
