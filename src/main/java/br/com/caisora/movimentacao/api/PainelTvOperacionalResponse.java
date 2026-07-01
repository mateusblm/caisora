package br.com.caisora.movimentacao.api;

import java.time.Instant;
import java.util.List;

public record PainelTvOperacionalResponse(
    Instant geradoEm,
    String fusoHorario,
    Instant inicioDia,
    Instant fimDia,
    int atualizarAposSegundos,
    ResumoPainelTvResponse resumo,
    List<MovimentacaoPainelTvResponse> alertas,
    List<MovimentacaoPainelTvResponse> descidasParaAgua,
    List<MovimentacaoPainelTvResponse> retiradasDaAgua,
    List<MovimentacaoPainelTvResponse> transferenciasDeVaga,
    List<MovimentacaoPainelTvResponse> deslocamentosInternos,
    List<MovimentacaoPainelTvResponse> emExecucao
) {

    public PainelTvOperacionalResponse {
        alertas = List.copyOf(alertas);
        descidasParaAgua =
            List.copyOf(descidasParaAgua);
        retiradasDaAgua =
            List.copyOf(retiradasDaAgua);
        transferenciasDeVaga =
            List.copyOf(transferenciasDeVaga);
        deslocamentosInternos =
            List.copyOf(deslocamentosInternos);
        emExecucao = List.copyOf(emExecucao);
    }
}
