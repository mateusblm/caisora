package br.com.caisora.referencia.infraestrutura.brasilapi;

import static br.com.caisora.referencia.infraestrutura.http.CamposJson.*;
import br.com.caisora.referencia.api.ReferenciasResponse.Feriado;
import br.com.caisora.referencia.infraestrutura.http.TransporteJsonExterno;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrasilApiClient {
    private final TransporteJsonExterno http;
    private final String baseUrl;
    public BrasilApiClient(TransporteJsonExterno http,
            @Value("${caisora.referencias.brasilapi-url:https://brasilapi.com.br}") String baseUrl) {
        this.http = http; this.baseUrl = baseUrl;
    }
    public List<Feriado> feriados(int ano, String uf) {
        var json = array(http.consultar(baseUrl, "/api/feriados/v1/" + ano, uf == null ? Map.of() : Map.of("uf", uf)));
        var resultado = new ArrayList<Feriado>();
        for (var feriado : json) {
            objeto(feriado);
            String data = texto(feriado.path("date"));
            LocalDate dia = data == null ? null : LocalDate.parse(data);
            if (dia != null && dia.getYear() != ano) throw new br.com.caisora.referencia.aplicacao.ReferenciaIndisponivelException();
            resultado.add(new Feriado(dia, texto(feriado.path("name")), texto(feriado.path("type")),
                    texto(feriado.path("state")), texto(feriado.path("weekday"))));
        }
        return resultado;
    }
}
