package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import java.time.Instant;
import java.util.UUID;

public record PosicaoEmbarcacaoResponse(
    UUID id,

    UUID embarcacaoId,
    String embarcacaoNome,
    String embarcacaoModelo,
    String proprietarioNome,

    TipoPosicaoEmbarcacao tipo,

    UUID vagaId,
    String vagaCodigo,
    String vagaSetor,
    String vagaLocalizacao,

    String descricaoLocal,
    UUID movimentacaoOrigemId,

    Long versao,
    UUID organizacaoId,
    Instant criadaEm,
    Instant atualizadaEm
) {
}
