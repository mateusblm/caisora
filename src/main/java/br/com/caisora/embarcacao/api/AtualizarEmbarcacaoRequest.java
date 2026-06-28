package br.com.caisora.embarcacao.api;

import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AtualizarEmbarcacaoRequest(

        @NotNull
        UUID proprietarioId,

        @Size(max = 150)
        String nome,

        @NotNull
        TipoEmbarcacao tipo,

        @Size(max = 100)
        String fabricante,

        @Size(max = 100)
        String modelo,

        @Min(1800)
        Integer anoFabricacao,

        @Size(max = 50)
        String numeroInscricao,

        @Size(max = 100)
        String numeroCasco,

        @Size(max = 150)
        String portoInscricao,

        @Pattern(regexp = "^[A-Za-z]{2}$")
        String codigoPaisBandeira,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal comprimentoTotalMetros,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal bocaMetros,

        @DecimalMin(value = "0.01")
        BigDecimal caladoMetros,

        @DecimalMin(value = "0.01")
        BigDecimal pontalMetros,

        @DecimalMin(value = "0.01")
        BigDecimal alturaTotalMetros,

        @DecimalMin(value = "0.01")
        BigDecimal pesoKg,

        @Min(1)
        Integer capacidadePessoas,

        @NotNull
        TipoPropulsao tipoPropulsao,

        @Size(max = 50)
        String corPredominante,

        @Size(max = 2000)
        String observacoes

) {
}