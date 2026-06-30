package br.com.caisora.movimentacao.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CancelarMovimentacaoRequest(
    @NotNull
    Instant canceladaEm,

    @NotBlank
    @Size(max = 1000)
    String motivo
) {
}
