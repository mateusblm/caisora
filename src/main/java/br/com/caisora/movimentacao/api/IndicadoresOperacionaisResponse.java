package br.com.caisora.movimentacao.api;

public record IndicadoresOperacionaisResponse(
    long emExecucao,
    long atrasadas,
    long proximaHora,
    long urgentes,
    long semOperador,
    long concluidasHoje
) {
}
