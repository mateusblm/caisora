package br.com.caisora.vaga.aplicacao;

import br.com.caisora.vaga.api.VagaResponse;
import br.com.caisora.vaga.dominio.Vaga;
import org.springframework.stereotype.Component;

@Component
public class VagaMapper {

    public VagaResponse paraResponse(
        Vaga vaga
    ) {
        return new VagaResponse(
            vaga.getId(),
            vaga.getCodigo(),
            vaga.getTipo(),
            vaga.getSetor(),
            vaga.getLocalizacao(),
            vaga.getComprimentoMaximoMetros(),
            vaga.getBocaMaximaMetros(),
            vaga.getCaladoMaximoMetros(),
            vaga.getAlturaMaximaMetros(),
            vaga.getPesoMaximoKg(),
            vaga.isPossuiAgua(),
            vaga.isPossuiEnergia(),
            vaga.getObservacoes(),
            vaga.isAtiva(),
            vaga.getOrganizacao().getId(),
            vaga.getCriadaEm(),
            vaga.getAtualizadaEm()
        );
    }
}