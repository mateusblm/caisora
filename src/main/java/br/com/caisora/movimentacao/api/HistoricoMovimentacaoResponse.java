package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoEventoMovimentacao;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HistoricoMovimentacaoResponse(
    UUID id,
    UUID movimentacaoId,

    TipoEventoMovimentacao tipoEvento,
    StatusMovimentacao statusAnterior,
    StatusMovimentacao statusNovo,

    Instant agendadaParaAnterior,
    Instant agendadaParaNova,

    UUID usuarioId,
    String usuarioNome,

    String observacao,
    Map<String, Object> dadosAnteriores,
    Map<String, Object> dadosNovos,

    UUID organizacaoId,
    Instant ocorridoEm
) {
}
