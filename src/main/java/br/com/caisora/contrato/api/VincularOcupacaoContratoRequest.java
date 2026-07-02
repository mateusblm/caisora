package br.com.caisora.contrato.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VincularOcupacaoContratoRequest(
    @NotNull UUID ocupacaoId
) {
}
