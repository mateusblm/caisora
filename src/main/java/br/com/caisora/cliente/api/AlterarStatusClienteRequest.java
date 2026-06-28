package br.com.caisora.cliente.api;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusClienteRequest(
    @NotNull Boolean ativo
) {
}