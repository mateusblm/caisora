package br.com.caisora.referencia.infraestrutura.ibge;

import static br.com.caisora.referencia.infraestrutura.http.CamposJson.*;
import br.com.caisora.referencia.api.ReferenciasResponse.*;
import br.com.caisora.referencia.infraestrutura.http.TransporteJsonExterno;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IbgeClient {
    private final TransporteJsonExterno http;
    private final String baseUrl;
    public IbgeClient(TransporteJsonExterno http,
            @Value("${caisora.referencias.ibge-url:https://servicodados.ibge.gov.br}") String baseUrl) {
        this.http = http; this.baseUrl = baseUrl;
    }
    public List<Pais> paises() {
        var json = array(http.consultar(baseUrl, "/api/v1/paises/-", Map.of()));
        var resultado = new ArrayList<Pais>();
        for (var pais : json) {
            objeto(pais);
            var id = pais.path("id");
            resultado.add(new Pais(inteiro(id.path("M49")), texto(id.path("ISO-3166-1-ALPHA-2")),
                    texto(id.path("ISO-3166-1-ALPHA-3")), texto(pais.path("nome").path("abreviado"))));
        }
        return resultado;
    }
    public List<Municipio> municipios(String uf) {
        var json = array(http.consultar(baseUrl, "/api/v1/localidades/estados/" + uf + "/municipios", Map.of()));
        var resultado = new ArrayList<Municipio>();
        for (var municipio : json) {
            objeto(municipio);
            var estado = municipio.path("microrregiao").path("mesorregiao").path("UF");
            if (!estado.isObject()) estado = municipio.path("regiao-imediata").path("regiao-intermediaria").path("UF");
            resultado.add(new Municipio(inteiro(municipio.path("id")), texto(municipio.path("nome")),
                    inteiro(estado.path("id")), texto(estado.path("sigla")), texto(estado.path("nome"))));
        }
        return resultado;
    }
}
