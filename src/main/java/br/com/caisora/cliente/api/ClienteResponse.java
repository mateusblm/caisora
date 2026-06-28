package br.com.caisora.cliente.api;

import br.com.caisora.cliente.dominio.TipoPessoa;

import java.time.Instant;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    TipoPessoa tipoPessoa,
    String nome,
    String razaoSocial,
    String cpfCnpj,
    String email,
    String telefone,
    String celular,
    String observacoes,
    boolean ativo,
    UUID organizacaoId,
    Instant criadoEm,
    Instant atualizadoEm
) {
}