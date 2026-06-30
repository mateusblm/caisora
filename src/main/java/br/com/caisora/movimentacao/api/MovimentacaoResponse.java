package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import java.time.Instant;
import java.util.UUID;

public record MovimentacaoResponse(
    UUID id,

    UUID embarcacaoId,
    String embarcacaoNome,
    String embarcacaoModelo,
    String proprietarioNome,

    TipoMovimentacao tipo,
    StatusMovimentacao status,
    PrioridadeMovimentacao prioridade,

    TipoPosicaoEmbarcacao
        tipoPosicaoOrigem,

    UUID vagaOrigemId,
    String vagaOrigemCodigo,
    String descricaoOrigem,

    TipoPosicaoEmbarcacao
        tipoPosicaoDestino,

    UUID vagaDestinoId,
    String vagaDestinoCodigo,
    String descricaoDestino,

    Instant agendadaPara,
    Instant iniciadaEm,
    Instant concluidaEm,
    Instant canceladaEm,

    UUID solicitadaPorId,
    String solicitadaPorNome,

    UUID operadorResponsavelId,
    String operadorResponsavelNome,

    String observacoes,
    String motivoCancelamento,

    Long versao,
    UUID organizacaoId,
    Instant criadaEm,
    Instant atualizadaEm
) {
}
