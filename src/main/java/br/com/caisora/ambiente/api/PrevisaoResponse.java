package br.com.caisora.ambiente.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PrevisaoResponse(String origem, String fonte, Instant consultadoEm, String fusoHorario,
String atualizacaoProvedor, boolean incompleta, Map<String,String> unidades, List<Hora> horas) {
    public record Hora(String horario, Map<String,Double> valores) {
    }
}
