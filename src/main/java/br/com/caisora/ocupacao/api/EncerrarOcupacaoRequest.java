package br.com.caisora.ocupacao.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EncerrarOcupacaoRequest(

    @NotNull
    Instant encerradaEm

) {

}