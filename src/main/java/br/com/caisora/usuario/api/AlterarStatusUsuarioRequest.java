package br.com.caisora.usuario.api;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusUsuarioRequest(
        @NotNull(message = "Status ativo e obrigatorio")
        Boolean ativo
) {
}
