package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Retorno: barco que estava na area de servico ou fora da marina volta pra vaga. */
public class RegraRetornoParaVaga implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        boolean origemValida =
            origem == TipoPosicaoEmbarcacao.AREA_SERVICO
            || origem == TipoPosicaoEmbarcacao.EXTERNA;

        if (!origemValida || destino != TipoPosicaoEmbarcacao.VAGA) {
            throw new IllegalArgumentException(
                "Retorno para a vaga deve sair da area de servico "
                    + "ou de area externa e terminar na vaga da ocupacao ativa"
            );
        }
    }
}
