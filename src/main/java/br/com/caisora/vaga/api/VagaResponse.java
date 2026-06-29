package br.com.caisora.vaga.api;

import br.com.caisora.vaga.dominio.TipoVaga;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VagaResponse(

    UUID id,

    String codigo,

    TipoVaga tipo,

    String setor,

    String localizacao,

    BigDecimal comprimentoMaximoMetros,

    BigDecimal bocaMaximaMetros,

    BigDecimal caladoMaximoMetros,

    BigDecimal alturaMaximaMetros,

    BigDecimal pesoMaximoKg,

    boolean possuiAgua,

    boolean possuiEnergia,

    String observacoes,

    boolean ativa,

    UUID organizacaoId,

    Instant criadaEm,

    Instant atualizadaEm

) {

}