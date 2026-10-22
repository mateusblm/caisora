package br.com.caisora.referencia.infraestrutura.http;

import br.com.caisora.referencia.aplicacao.ReferenciaIndisponivelException;
import tools.jackson.databind.JsonNode;

public final class CamposJson {
    private CamposJson() {}
    public static String texto(JsonNode n) { return n.isString() && !n.asString().isBlank() ? n.asString() : null; }
    public static Integer inteiro(JsonNode n) { return n.isIntegralNumber() ? n.asInt() : null; }
    public static Double decimal(JsonNode n) { return n.isNumber() && Double.isFinite(n.asDouble()) ? n.asDouble() : null; }
    public static JsonNode array(JsonNode n) {
        if (n == null || !n.isArray()) throw new ReferenciaIndisponivelException();
        return n;
    }
    public static void objeto(JsonNode n) { if (!n.isObject()) throw new ReferenciaIndisponivelException(); }
}
