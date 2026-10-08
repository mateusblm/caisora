package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoMovimentacao;

/** Seleciona a Strategy; a validação fica nas classes concretas, não neste seletor. */
public final class RegrasMovimentacao {
    private RegrasMovimentacao() { }

    public static RegraMovimentacao para(TipoMovimentacao tipo) {
        return switch (tipo) {
            case LANCAMENTO -> new RegraLancamento();
            case RETIRADA -> new RegraRetirada();
            case RETORNO_PARA_VAGA -> new RegraRetornoParaVaga();
            case TRANSFERENCIA -> new RegraTransferencia();
            case DESLOCAMENTO_INTERNO -> new RegraDeslocamentoInterno();
        };
    }
}
