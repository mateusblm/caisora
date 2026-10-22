package br.com.caisora.ambiente.infraestrutura.openmeteo;

import br.com.caisora.ambiente.api.PrevisaoResponse;
import br.com.caisora.ambiente.aplicacao.ProvedorIndisponivelException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class OpenMeteoClient {

    private static final List<String> WEATHER = List.of(
            "temperature_2m", "wind_speed_10m", "wind_direction_10m",
            "wind_gusts_10m", "precipitation_probability");
    private static final List<String> MARINE = List.of(
            "wave_height", "wave_direction", "wave_period",
            "swell_wave_height", "swell_wave_direction");

    private final RestClient weather;
    private final RestClient marine;
    private final String apiKey;

    public OpenMeteoClient(
            @Value("${caisora.open-meteo.weather-url:https://api.open-meteo.com}") String weatherUrl,
            @Value("${caisora.open-meteo.marine-url:https://marine-api.open-meteo.com}") String marineUrl,
            @Value("${caisora.open-meteo.api-key:}") String apiKey,
            @Value("${caisora.open-meteo.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${caisora.open-meteo.read-timeout-ms:8000}") int readTimeout
    ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        weather = RestClient.builder().baseUrl(weatherUrl).requestFactory(factory).build();
        marine = RestClient.builder().baseUrl(marineUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
    }

    public PrevisaoResponse consultar(
            boolean mar, Number latitude, Number longitude, LocalDate inicio, LocalDate fim
    ) {
        try {
            var campos = mar ? MARINE : WEATHER;
            JsonNode json = (mar ? marine : weather).get().uri(builder -> {
                builder.path(mar ? "/v1/marine" : "/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", String.join(",", campos))
                        .queryParam("timezone", "UTC")
                        .queryParam("start_date", inicio)
                        .queryParam("end_date", fim);
                if (!apiKey.isBlank()) {
                    builder.queryParam("apikey", apiKey);
                }
                return builder.build();
            }).retrieve().body(JsonNode.class);
            return interpretar(json, mar);
        } catch (RuntimeException exception) {
            // Não expõe a URL da requisição, que pode conter a chave comercial.
            throw new ProvedorIndisponivelException();
        }
    }

    private PrevisaoResponse interpretar(JsonNode json, boolean mar) {
        if (json == null || !json.path("hourly").path("time").isArray()) {
            throw new ProvedorIndisponivelException();
        }
        var hourly = json.path("hourly");
        var horarios = hourly.path("time");
        var campos = mar ? MARINE : WEATHER;
        var horas = new ArrayList<PrevisaoResponse.Hora>();
        var unidades = new LinkedHashMap<String, String>();
        boolean incompleta = horarios.isEmpty();

        for (String campo : campos) {
            var unidade = json.path("hourly_units").path(campo);
            unidades.put(campo, unidade.isString() ? unidade.asString() : "não informada");
            if (!unidade.isString() || !hourly.path(campo).isArray()
                    || hourly.path(campo).size() != horarios.size()) {
                incompleta = true;
            }
        }
        for (int i = 0; i < horarios.size(); i++) {
            if (!horarios.get(i).isString()) {
                throw new ProvedorIndisponivelException();
            }
            String horario = horarios.get(i).asString();
            LocalDateTime.parse(horario);
            var valores = new LinkedHashMap<String, Double>();
            for (String campo : campos) {
                var valor = hourly.path(campo).path(i);
                Double numero = valor.isNumber() ? valor.asDouble() : null;
                if (numero == null || !Double.isFinite(numero)) {
                    numero = null;
                    incompleta = true;
                }
                valores.put(campo, numero);
            }
            horas.add(new PrevisaoResponse.Hora(horario + "Z", valores));
        }
        return new PrevisaoResponse(
                "Open-Meteo",
                mar ? "https://open-meteo.com/en/docs/marine-weather-api" : "https://open-meteo.com/en/docs",
                Instant.now(), "UTC",
                "Horário de atualização do modelo não informado pelo provedor",
                incompleta, unidades, horas);
    }
}
