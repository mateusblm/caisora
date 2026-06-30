package br.com.caisora.movimentacao.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ConcluirMovimentacaoRequest(
    @NotNull
    Instant concluidaEm,

    @Size(max = 2000)
    String observacao
) {
}
