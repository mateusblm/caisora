package br.com.caisora.vaga.api;

import br.com.caisora.vaga.dominio.TipoVaga;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarVagaRequest(

    @NotBlank
    @Size(max = 50)
    String codigo,

    @NotNull
    TipoVaga tipo,

    @Size(max = 100)
    String setor,

    @Size(max = 200)
    String localizacao,

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal comprimentoMaximoMetros,

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal bocaMaximaMetros,

    @DecimalMin(value = "0.01")
    BigDecimal caladoMaximoMetros,

    @DecimalMin(value = "0.01")
    BigDecimal alturaMaximaMetros,

    @DecimalMin(value = "0.01")
    BigDecimal pesoMaximoKg,

    @NotNull
    Boolean possuiAgua,

    @NotNull
    Boolean possuiEnergia,

    @Size(max = 2000)
    String observacoes

) {

}