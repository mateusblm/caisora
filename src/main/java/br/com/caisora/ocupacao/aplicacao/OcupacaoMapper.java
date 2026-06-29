package br.com.caisora.ocupacao.aplicacao;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.ocupacao.api.OcupacaoResponse;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import org.springframework.stereotype.Component;

@Component
public class OcupacaoMapper {

    public OcupacaoResponse paraResponse(
        Ocupacao ocupacao
    ) {
        Embarcacao embarcacao =
            ocupacao.getEmbarcacao();

        return new OcupacaoResponse(
            ocupacao.getId(),

            embarcacao.getId(),
            obterNomeEmbarcacao(embarcacao),
            embarcacao.getModelo(),
            embarcacao
                .getProprietario()
                .getNome(),

            ocupacao.getVaga().getId(),
            ocupacao.getVaga().getCodigo(),
            ocupacao.getVaga().getTipo(),
            ocupacao.getVaga().getSetor(),
            ocupacao.getVaga().getLocalizacao(),

            ocupacao.getStatus(),
            ocupacao.getInicioEm(),
            ocupacao.getFimPrevistoEm(),
            ocupacao.getEncerradaEm(),
            ocupacao.getObservacoes(),

            ocupacao
                .getOrganizacao()
                .getId(),

            ocupacao.getCriadaEm(),
            ocupacao.getAtualizadaEm()
        );
    }

    private String obterNomeEmbarcacao(
        Embarcacao embarcacao
    ) {
        if (
            embarcacao.getNome() != null
                && !embarcacao.getNome().isBlank()
        ) {
            return embarcacao.getNome();
        }

        if (
            embarcacao.getModelo() != null
                && !embarcacao.getModelo().isBlank()
        ) {
            return embarcacao.getModelo();
        }

        if (
            embarcacao.getNumeroInscricao() != null
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