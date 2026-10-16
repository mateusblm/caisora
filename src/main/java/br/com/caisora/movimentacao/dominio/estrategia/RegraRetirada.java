package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Retirada: o caminho inverso do lancamento, da agua ou do pier de volta pra uma vaga. */
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
