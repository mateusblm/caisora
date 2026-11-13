package br.com.caisora.dbfirst;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class HttpCrudTest {
    static DemoServer app;
    static final HttpClient client = HttpClient.newHttpClient();
    static final ObjectMapper json = new ObjectMapper();
    static final String AUTH = "Basic " + Base64.getEncoder().encodeToString("demo:test-local".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    @BeforeAll static void start() throws Exception { DatabaseFirstTest.factory = Database.factory(); app = new DemoServer("127.0.0.1", 0, "test-local"); app.start(); }
    @AfterAll static void stop() { if (app != null) app.close(); }
    static HttpResponse<String> request(String route, String method, String body, boolean auth) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + route));
        if (auth) request.header("Authorization", AUTH);
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest @ValueSource(strings={"modulos", "permissoes"})
    void authenticatedHttpCrudAndConflict(String table) throws Exception {
        String route = "/api/" + table, label = "http-" + UUID.randomUUID(); UUID id = null;
        String body = table.equals("modulos") ? "{\"modulo\":\"" + label + "\",\"ativo\":true}" : "{\"modulo\":\"" + label + "\",\"perfil\":\"ATENDENTE\",\"acao\":\"CONSULTAR\",\"permitido\":true}";
        try {
            assertEquals(401, request(route, "GET", null, false).statusCode());
            assertEquals(401, request(route, "POST", body, false).statusCode());
            var created = request(route, "POST", body, true); assertEquals(201, created.statusCode(), created.body());
            id = UUID.fromString(json.readTree(created.body()).get("id").asText());
            assertEquals(Database.TENANT.toString(), json.readTree(created.body()).get("organizacaoId").asText());
            assertTrue(request(route, "GET", null, true).body().contains(id.toString()));
            assertEquals(409, request(route, "POST", body, true).statusCode());
            assertEquals(200, request(route + "/" + id, "GET", null, true).statusCode());
            var updated = request(route + "/" + id, "PUT", body.replace("true", "false"), true); assertEquals(200, updated.statusCode(), updated.body());
            assertFalse(DatabaseFirstTest.persistedFlag(table, id));
            assertEquals(204, request(route + "/" + id, "DELETE", null, true).statusCode());
            assertEquals(404, request(route + "/" + id, "GET", null, true).statusCode());
            assertEquals(404, request(route + "/" + id, "DELETE", null, true).statusCode());
            DatabaseFirstTest.absent(table, id);
        } finally { if (id != null) DatabaseFirstTest.cleanup(table, id); }
    }
    @Test void invalidPayloadsAndRoutesAreRejected() throws Exception {
        for (String body : List.of("{}", "null", "{", "{\"modulo\":\"\",\"ativo\":true}", "{\"modulo\":\"CLIENTES\",\"ativo\":\"true\"}", "{\"modulo\":\"CLIENTES\",\"ativo\":true,\"organizacaoId\":\"" + Database.OTHER_TENANT + "\"}", "{\"modulo\":\"" + "x".repeat(51) + "\",\"ativo\":true}")) {
            assertEquals(400, request("/api/modulos", "POST", body, true).statusCode(), body);
        }
        assertEquals(400, request("/api/modulos/not-a-uuid", "GET", null, true).statusCode());
        assertEquals(404, request("/api/unknown", "GET", null, true).statusCode());
        assertEquals(405, request("/api/modulos", "PATCH", "{}", true).statusCode());
        assertEquals(413, request("/api/modulos", "POST", " ".repeat(8193), true).statusCode());
        var page = request("/", "GET", null, false); assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Módulos da marina"));
        var brand = request("/brand.png", "GET", null, false); assertEquals(200, brand.statusCode());
        assertTrue(brand.headers().firstValue("Content-Type").orElse("").startsWith("image/png"));
        assertTrue(page.headers().firstValue("Content-Security-Policy").isPresent());
    }
}
