package br.com.caisora.vaga.api;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusVagaRequest(

    @NotNull
    Boolean ativa

) {

}