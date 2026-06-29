package br.com.caisora.organizacao.api;

import java.time.Instant;
import java.util.UUID;

public record OrganizacaoResponse(
        UUID id,
        String nome,
        String slug,
        String razaoSocial,
        String documento,
        String email,
        String telefone,
        boolean ativa,
        Instant criadaEm,
        Instant atualizadaEm
) {
}
