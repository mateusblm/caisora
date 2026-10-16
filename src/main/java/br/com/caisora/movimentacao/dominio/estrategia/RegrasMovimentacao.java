package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoMovimentacao;

/** Seleciona a Strategy; a validação fica nas classes concretas, não neste seletor. */
public final class RegrasMovimentacao {
    private RegrasMovimentacao() { }

    public static RegraMovimentacao para(TipoMovimentacao tipo) {
        // As regras nao guardam estado, entao criar uma nova a cada chamada nao tem custo.
        // O switch nao tem default: um TipoMovimentacao novo nao compila ate ganhar sua regra.
        return switch (tipo) {
            case LANCAMENTO -> new RegraLancamento();
            case RETIRADA -> new RegraRetirada();
            case RETORNO_PARA_VAGA -> new RegraRetornoParaVaga();
            case TRANSFERENCIA -> new RegraTransferencia();
            case DESLOCAMENTO_INTERNO -> new RegraDeslocamentoInterno();
        };
    }
}
