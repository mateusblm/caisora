package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.movimentacao.api.PosicaoEmbarcacaoResponse;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.vaga.dominio.Vaga;
import org.springframework.stereotype.Component;

@Component
public class PosicaoEmbarcacaoMapper {

    public PosicaoEmbarcacaoResponse
    paraResponse(
        PosicaoEmbarcacao posicao
    ) {
        Embarcacao embarcacao =
            posicao.getEmbarcacao();

        Vaga vaga =
            posicao.getVaga();

        Movimentacao movimentacaoOrigem =
            posicao.getMovimentacaoOrigem();

        return new PosicaoEmbarcacaoResponse(
            posicao.getId(),

            embarcacao.getId(),
            obterNomeEmbarcacao(
                embarcacao
            ),
            embarcacao.getModelo(),
            embarcacao
                .getProprietario()
                .getNome(),

            posicao.getTipo(),

            vaga == null
                ? null
                : vaga.getId(),

            vaga == null
                ? null
                : vaga.getCodigo(),

            vaga == null
                ? null
                : vaga.getSetor(),

            vaga == null
                ? null
                : vaga.getLocalizacao(),

            posicao.getDescricaoLocal(),

            movimentacaoOrigem == null
                ? null
                : movimentacaoOrigem.getId(),

            posicao.getVersao(),
            posicao
                .getOrganizacao()
                .getId(),
            posicao.getCriadaEm(),
            posicao.getAtualizadaEm()
        );
    }

    private String obterNomeEmbarcacao(
        Embarcacao embarcacao
    ) {
        if (
            embarcacao.getNome() != null
            && !embarcacao.getNome()
                .isBlank()
        ) {
            return embarcacao.getNome();
        }

        if (
            embarcacao.getModelo() != null
            && !embarcacao.getModelo()
                .isBlank()
        ) {
            return embarcacao.getModelo();
        }

        if (
            embarcacao
                .getNumeroInscricao()
                != null
            && !embarcacao
                .getNumeroInscricao()
                .isBlank()
        ) {
            return embarcacao
                .getNumeroInscricao();
        }

        return "Embarcação sem nome";
    }
}
