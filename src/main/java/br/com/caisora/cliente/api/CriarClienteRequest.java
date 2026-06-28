package br.com.caisora.cliente.api;

import br.com.caisora.cliente.dominio.TipoPessoa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarClienteRequest(

    @NotNull
    TipoPessoa tipoPessoa,

    @NotBlank
    @Size(max = 150)
    String nome,

    @Size(max = 200)
    String razaoSocial,

    @NotBlank
    @Size(max = 18)
    String cpfCnpj,

    @Email
    @Size(max = 150)
    String email,

    @Size(max = 20)
    String telefone,

    @Size(max = 20)
    String celular,

    @Size(max = 2000)
    String observacoes

) {
}