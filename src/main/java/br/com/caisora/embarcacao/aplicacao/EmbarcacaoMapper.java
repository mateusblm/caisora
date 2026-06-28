package br.com.caisora.embarcacao.aplicacao;

import br.com.caisora.embarcacao.api.EmbarcacaoResponse;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import org.springframework.stereotype.Component;

@Component
public class EmbarcacaoMapper {

    public EmbarcacaoResponse paraResponse(
            Embarcacao embarcacao
    ) {
        return new EmbarcacaoResponse(
                embarcacao.getId(),

                embarcacao.getProprietario().getId(),
                embarcacao.getProprietario().getNome(),

                embarcacao.getNome(),
                embarcacao.getTipo(),

                embarcacao.getFabricante(),
                embarcacao.getModelo(),
                embarcacao.getAnoFabricacao(),

                embarcacao.getNumeroInscricao(),
                embarcacao.getNumeroCasco(),
                embarcacao.getPortoInscricao(),
                embarcacao.getCodigoPaisBandeira(),

                embarcacao.getComprimentoTotalMetros(),
                embarcacao.getBocaMetros(),
                embarcacao.getCaladoMetros(),
                embarcacao.getPontalMetros(),
                embarcacao.getAlturaTotalMetros(),
                embarcacao.getPesoKg(),

                embarcacao.getCapacidadePessoas(),
                embarcacao.getTipoPropulsao(),

                embarcacao.getCorPredominante(),
                embarcacao.getObservacoes(),

                embarcacao.isAtiva(),

                embarcacao.getOrganizacao().getId(),
                embarcacao.getCriadaEm(),
                embarcacao.getAtualizadaEm()
        );
    }
}