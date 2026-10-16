package br.com.caisora.movimentacao.dominio.estrategia;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;

/** Deslocamento interno: so valida o destino (pier, area de servico ou externa); a origem pode ser qualquer uma. */
public class RegraDeslocamentoInterno implements RegraMovimentacao {
    @Override
    public void validar(TipoPosicaoEmbarcacao origem, Vaga vagaOrigem, TipoPosicaoEmbarcacao destino, Vaga vagaDestino) {
        if (
            destino == TipoPosicaoEmbarcacao.VAGA
            || destino == TipoPosicaoEmbarcacao.AGUA
            || destino == TipoPosicaoEmbarcacao.DESCONHECIDA
        ) {
            throw new IllegalArgumentException(
                "Deslocamento interno deve terminar no pier, area de servico ou area externa"
            );
        }
    }
}
