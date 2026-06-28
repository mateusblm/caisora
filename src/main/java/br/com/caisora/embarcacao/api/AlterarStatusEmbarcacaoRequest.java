package br.com.caisora.embarcacao.api;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusEmbarcacaoRequest(
        @NotNull Boolean ativa
) {
}