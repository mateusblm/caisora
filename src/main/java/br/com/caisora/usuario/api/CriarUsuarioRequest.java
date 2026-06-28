package br.com.caisora.usuario.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 150, message = "E-mail deve ter no maximo 150 caracteres")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String senha,

        @NotNull(message = "Perfil e obrigatorio")
        PerfilUsuario perfil
) {
}
