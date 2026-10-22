package br.com.caisora.referencia.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReferenciasResponse {
    private ReferenciasResponse() {}
    public record Consulta<T>(String origem, String fonte, Instant consultadoEm, boolean incompleta, List<T> registros) {}
    public record Pais(Integer codigoM49, String codigoIso2, String codigoIso3, String nome) {}
    public record Municipio(Integer codigoIbge, String nome, Integer codigoUf, String siglaUf, String nomeUf) {}
    public record Feriado(LocalDate data, String nome, String tipo, String uf, String diaSemana) {}
    public record EventoMare(Instant dataHora, String tipo, Double altura, String unidade) {}
    public record Mares(String origem, String fonte, Instant consultadoEm, boolean incompleta,
            String datumSolicitado, String datumResposta, String copyright,
            Double latitudeResposta, Double longitudeResposta, List<EventoMare> registros) {}
    public record DisponibilidadeMares(boolean habilitada, String mensagem) {}
}
