package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Regra específica reutilizada tanto na criação quanto na edição da ordem. */
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
