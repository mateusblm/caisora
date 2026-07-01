package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MovimentacaoPainelTvResponse(
    UUID id,
    UUID embarcacaoId,
    String embarcacaoNome,
    String embarcacaoModelo,
    String proprietarioNome,
    TipoMovimentacao tipo,
    AcaoOperacionalPainelTv acaoOperacional,
    StatusMovimentacao status,
    PrioridadeMovimentacao prioridade,
    SituacaoPainelTv situacao,
    LocalMovimentacaoPainelTvResponse origem,
    LocalMovimentacaoPainelTvResponse destino,
    Instant agendadaPara,
    Instant iniciadaEm,
    String operadorResponsavelNome,
    String observacoes,
    long minutosAtraso,
    long minutosParaInicio,
    long minutosEmExecucao,
    List<TipoAlertaPainelTv> alertas
) {

    public MovimentacaoPainelTvResponse {
        alertas = List.copyOf(alertas);
    }
}
