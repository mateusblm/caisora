package br.com.caisora.movimentacao.api;

import java.time.Instant;
import java.util.List;

public record PainelOperacionalResponse(
    Instant geradoEm,
    String fusoHorario,
    Instant inicioDia,
    Instant fimDia,
    IndicadoresOperacionaisResponse indicadores,
    List<MovimentacaoResponse> atrasadas,
    List<MovimentacaoResponse> emExecucao,
    List<MovimentacaoResponse> proximosTrintaMinutos,
    List<MovimentacaoResponse> proximasDuasHoras,
    List<MovimentacaoResponse> restanteDia,
    List<MovimentacaoResponse> concluidasRecentemente
) {

    public PainelOperacionalResponse {
        atrasadas = List.copyOf(atrasadas);
        emExecucao = List.copyOf(emExecucao);
        proximosTrintaMinutos =
            List.copyOf(proximosTrintaMinutos);
        proximasDuasHoras =
            List.copyOf(proximasDuasHoras);
        restanteDia = List.copyOf(restanteDia);
        concluidasRecentemente =
            List.copyOf(concluidasRecentemente);
    }
}
