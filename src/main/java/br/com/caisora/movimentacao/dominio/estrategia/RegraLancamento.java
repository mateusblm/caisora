package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Regra específica reutilizada tanto na criação quanto na edição da ordem. */
public class RegraLancamento implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        if (
            origem != TipoPosicaoEmbarcacao.VAGA
            || (
                destino != TipoPosicaoEmbarcacao.AGUA
                && destino != TipoPosicaoEmbarcacao.PIER_ESPERA
            )
        ) {
            throw new IllegalArgumentException(
                "Lancamento deve sair de uma vaga e terminar na agua ou no pier de espera"
            );
        }
    }
}
