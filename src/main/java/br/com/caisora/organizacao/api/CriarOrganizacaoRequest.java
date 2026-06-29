package br.com.caisora.organizacao.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarOrganizacaoRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "Codigo da marina e obrigatorio")
        @Size(max = 80, message = "Codigo da marina deve ter no maximo 80 caracteres")
        String slug,

        @Size(max = 200, message = "Razao social deve ter no maximo 200 caracteres")
        String razaoSocial,

        @Size(max = 30, message = "Documento deve ter no maximo 30 caracteres")
        String documento,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 150, message = "E-mail deve ter no maximo 150 caracteres")
        String email,

        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String telefone
) {
}
