package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CriarMovimentacaoRequest(
    @NotNull
    UUID embarcacaoId,

    @NotNull
    TipoMovimentacao tipo,

    @NotNull
    PrioridadeMovimentacao prioridade,

    @NotNull
    TipoPosicaoEmbarcacao
        tipoPosicaoDestino,

    UUID vagaDestinoId,

    @Size(max = 255)
    String descricaoDestino,

    @NotNull
    Instant agendadaPara,

    UUID operadorResponsavelId,

    @Size(max = 2000)
    String observacoes
) {
}
