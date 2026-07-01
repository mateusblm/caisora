package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;

public record LocalMovimentacaoPainelTvResponse(
    TipoPosicaoEmbarcacao tipo,
    String vagaCodigo,
    String descricao,
    String rotulo
) {
}
