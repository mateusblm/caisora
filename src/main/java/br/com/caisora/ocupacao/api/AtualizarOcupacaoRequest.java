package br.com.caisora.ocupacao.api;

import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AtualizarOcupacaoRequest(

    Instant fimPrevistoEm,

    @Size(max = 2000)
    String observacoes

) {

}