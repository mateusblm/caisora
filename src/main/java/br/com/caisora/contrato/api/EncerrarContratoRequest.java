package br.com.caisora.contrato.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EncerrarContratoRequest(
    @NotNull LocalDate dataEncerramento,
    @Size(max = 1000) String descricao
) {
}
