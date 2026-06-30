package br.com.caisora.movimentacao.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record IniciarMovimentacaoRequest(
    @NotNull
    Instant iniciadaEm,

    @Size(max = 2000)
    String observacao
) {
}
