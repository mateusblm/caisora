package br.com.caisora.contrato.api;

import br.com.caisora.contrato.dominio.PeriodicidadeContrato;
import br.com.caisora.vaga.dominio.TipoVaga;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AtualizarContratoRequest(
    @NotNull UUID clienteId,
    @NotNull UUID embarcacaoId,
    @NotNull TipoVaga tipoVagaContratada,
    @NotNull PeriodicidadeContrato periodicidade,
    @NotNull LocalDate dataInicio,
    LocalDate dataFim,
    boolean renovacaoAutomatica,
    @Min(0) Integer diasAvisoPrevio,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal valorBase,
    @NotNull @Min(1) @Max(31) Integer diaVencimento,
    @Size(max = 2000) String observacoes
) {
}
