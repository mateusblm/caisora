package br.com.caisora.contrato.api;

import br.com.caisora.contrato.dominio.StatusContrato;
import br.com.caisora.contrato.dominio.TipoEventoContrato;
import java.time.Instant;
import java.util.UUID;

public record HistoricoContratoResponse(
    UUID id,
    UUID contratoId,
    TipoEventoContrato tipoEvento,
    StatusContrato statusAnterior,
    StatusContrato statusNovo,
    String descricao,
    UUID realizadoPorId,
    String realizadoPorNome,
    Instant realizadoEm
) {
}
