package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Regra específica reutilizada tanto na criação quanto na edição da ordem. */
public class RegraRetirada implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        boolean origemValida =
            origem == TipoPosicaoEmbarcacao.AGUA
            || origem == TipoPosicaoEmbarcacao.PIER_ESPERA;

        if (!origemValida || destino != TipoPosicaoEmbarcacao.VAGA) {
            throw new IllegalArgumentException(
                "Retirada deve sair da agua ou do pier e terminar em uma vaga"
            );
        }
    }
}
