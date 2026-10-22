package br.com.caisora.referencia.infraestrutura.worldtides;

import static br.com.caisora.referencia.infraestrutura.http.CamposJson.*;
import br.com.caisora.referencia.api.ReferenciasResponse.*;
import br.com.caisora.referencia.aplicacao.ReferenciaIndisponivelException;
import br.com.caisora.referencia.infraestrutura.http.TransporteJsonExterno;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorldTidesClient {
    private final TransporteJsonExterno http;
    private final String baseUrl;
    private final String chave;
    private final boolean habilitado;
    private final boolean usoAutorizado;
    public WorldTidesClient(TransporteJsonExterno http,
            @Value("${caisora.referencias.worldtides-url:https://www.worldtides.info}") String baseUrl,
            @Value("${caisora.referencias.worldtides-key:}") String chave,
            @Value("${caisora.referencias.worldtides-habilitado:false}") boolean habilitado,
            @Value("${caisora.referencias.worldtides-uso-autorizado:false}") boolean usoAutorizado) {
        this.http = http; this.baseUrl = baseUrl; this.chave = chave;
        this.habilitado = habilitado; this.usoAutorizado = usoAutorizado;
    }
    public boolean disponivel() { return habilitado && usoAutorizado && !chave.isBlank(); }
    public Mares consultar(BigDecimal latitude, BigDecimal longitude, LocalDate inicio, int dias) {
        // Segunda trava protege também chamadas diretas ao cliente, sem passar pelo serviço.
        if (!disponivel()) throw new br.com.caisora.compartilhado.excecao.DadosInvalidosException(
                "MARES_DESABILITADAS", "WorldTides desabilitada: exige credenciais e autorização de uso pela plataforma");
        var json = http.consultar(baseUrl, "/api/v3", Map.of("extremes", "", "date", inicio.toString(),
                "days", Integer.toString(dias), "lat", latitude.toPlainString(), "lon", longitude.toPlainString(),
                "datum", "MSL", "key", chave));
        if (json == null || json.path("status").asInt() != 200) throw new ReferenciaIndisponivelException();
        String copyright = texto(json.path("copyright"));
        if (copyright == null) throw new ReferenciaIndisponivelException();
        var eventos = new ArrayList<EventoMare>();
        boolean incompleta = texto(json.path("responseDatum")) == null;
        for (var e : array(json.path("extremes"))) {
            objeto(e);
            String data = texto(e.path("date"));
            Instant hora = e.path("dt").isIntegralNumber() ? Instant.ofEpochSecond(e.path("dt").asLong())
                    : data == null ? null : interpretarData(data);
            String tipo = texto(e.path("type"));
            Double altura = decimal(e.path("height"));
            incompleta |= hora == null || tipo == null || altura == null;
            // Unidade fixa do contrato da API v3: height é fornecida em metros.
            eventos.add(new EventoMare(hora, tipo, altura, "m"));
        }
        return new Mares("WorldTides", "https://www.worldtides.info/apidocs", Instant.now(), incompleta,
                texto(json.path("requestDatum")), texto(json.path("responseDatum")), copyright,
                decimal(json.path("responseLat")), decimal(json.path("responseLon")), eventos);
    }
    private Instant interpretarData(String data) {
        try { return OffsetDateTime.parse(data).toInstant(); }
        catch (java.time.format.DateTimeParseException exception) {
            return OffsetDateTime.parse(data, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")).toInstant();
        }
    }

}
