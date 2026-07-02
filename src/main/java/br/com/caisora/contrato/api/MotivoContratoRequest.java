package br.com.caisora.contrato.api;

import jakarta.validation.constraints.Size;

public record MotivoContratoRequest(
    @Size(max = 1000) String descricao
) {
}
