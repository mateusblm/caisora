package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;
import java.util.Objects;

/** Regra específica reutilizada tanto na criação quanto na edição da ordem. */
public class RegraTransferencia implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        if (
            origem != TipoPosicaoEmbarcacao.VAGA
            || destino != TipoPosicaoEmbarcacao.VAGA
            || vagaOrigem == null
            || vagaDestino == null
            || Objects.equals(vagaOrigem.getId(), vagaDestino.getId())
        ) {
            throw new IllegalArgumentException(
                "Transferencia exige vagas de origem e destino diferentes"
            );
        }
    }
}
