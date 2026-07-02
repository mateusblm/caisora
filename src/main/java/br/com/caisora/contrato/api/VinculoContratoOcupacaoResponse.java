package br.com.caisora.contrato.api;

import java.time.Instant;
import java.util.UUID;

public record VinculoContratoOcupacaoResponse(
    UUID id,
    UUID contratoId,
    UUID ocupacaoId,
    UUID vagaId,
    String vagaCodigo,
    Instant inicioEm,
    Instant fimEm,
    String motivoFim,
    boolean aberto
) {
}
