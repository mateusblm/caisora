package br.com.caisora.variabilidade.api;

import jakarta.validation.constraints.NotNull;

public record AtualizarModuloRequest(
        @NotNull Boolean ativo
) {
}
