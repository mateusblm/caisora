package br.com.caisora.referencia.infraestrutura.http;

import br.com.caisora.referencia.aplicacao.ReferenciaIndisponivelException;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TransporteJsonExterno {
    private static final int LIMITE_BYTES = 8 * 1024 * 1024;
    private final RestClient http;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public TransporteJsonExterno(
            @Value("${caisora.referencias.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${caisora.referencias.read-timeout-ms:10000}") int readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        http = RestClient.builder().requestFactory(factory).build();
    }

    public JsonNode consultar(String baseUrl, String caminho, Map<String, String> parametros) {
        try {
            var uri = UriComponentsBuilder.fromUriString(baseUrl).path(caminho);
            parametros.forEach((nome, valor) -> {
                if (valor.isEmpty()) uri.queryParam(nome);
                else uri.queryParam(nome, valor);
            });
            return http.get().uri(uri.build().encode().toUri()).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) throw new ReferenciaIndisponivelException();
                byte[] bytes = response.getBody().readNBytes(LIMITE_BYTES + 1);
                if (bytes.length > LIMITE_BYTES) throw new ReferenciaIndisponivelException();
                // O IBGE pode entregar gzip mesmo sem negociação explícita do cliente.
                if (bytes.length >= 2 && bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b) {
                    try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                        bytes = gzip.readNBytes(LIMITE_BYTES + 1);
                    }
                }
                if (bytes.length > LIMITE_BYTES) throw new ReferenciaIndisponivelException();
                return mapper.readTree(bytes);
            });
        } catch (Exception exception) {
            // Não propaga URL, corpo de erro ou causa: WorldTides leva a chave na query string.
            throw new ReferenciaIndisponivelException();
        }
    }
}
