package br.com.caisora.organizacao.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CoordenadasRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180")
        @Digits(integer = 3, fraction = 6) BigDecimal longitude
) {
}
