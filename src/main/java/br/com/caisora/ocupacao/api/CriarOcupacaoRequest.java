package br.com.caisora.ocupacao.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CriarOcupacaoRequest(

    @NotNull
    UUID embarcacaoId,

    @NotNull
    UUID vagaId,

    @NotNull
    Instant inicioEm,

    Instant fimPrevistoEm,

    @Size(max = 2000)
    String observacoes

) {

}