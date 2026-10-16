package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;
import java.util.Objects;

/** Transferencia: troca de vaga. Unica regra que olha as vagas, nao so o tipo de posicao. */
public class RegraTransferencia implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        if (
            origem != TipoPosicaoEmbarcacao.VAGA
            || destino != TipoPosicaoEmbarcacao.VAGA
            || vagaOrigem == null
            || vagaDestino == null
            // Compara pelo id porque Vaga nao sobrescreve equals.
            || Objects.equals(vagaOrigem.getId(), vagaDestino.getId())
        ) {
            throw new IllegalArgumentException(
                "Transferencia exige vagas de origem e destino diferentes"
            );
        }
    }
}
