package br.com.caisora.usuario.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @NotNull(message = "Perfil e obrigatorio")
        PerfilUsuario perfil
) {
}
