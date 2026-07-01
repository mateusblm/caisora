package br.com.caisora.movimentacao.api;

public record ResumoPainelTvResponse(
    long descidasParaAgua,
    long retiradasDaAgua,
    long transferenciasDeVaga,
    long deslocamentosInternos,
    long emExecucao,
    long alertas
) {
}
