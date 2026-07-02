package br.com.caisora.contrato.api;

import br.com.caisora.contrato.dominio.PeriodicidadeContrato;
import br.com.caisora.contrato.dominio.StatusContrato;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResponse(
    UUID id,
    String numero,
    StatusContrato status,
    UUID clienteId,
    String clienteNome,
    UUID embarcacaoId,
    String embarcacaoNome,
    TipoVaga tipoVagaContratada,
    PeriodicidadeContrato periodicidade,
    LocalDate dataInicio,
    LocalDate dataFim,
    LocalDate dataAssinatura,
    LocalDate dataAtivacao,
    LocalDate dataEncerramento,
    boolean renovacaoAutomatica,
    Integer diasAvisoPrevio,
    BigDecimal valorBase,
    Integer diaVencimento,
    String observacoes,
    UUID criadoPorId,
    String criadoPorNome,
    UUID organizacaoId,
    Instant criadoEm,
    Instant atualizadoEm,
    Long versao
) {
}
