package br.com.caisora.dbfirst;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;

/** HTTP e interface manuais, fora do Spring/JPA do produto. */
public final class DemoServer implements AutoCloseable {
    private final HttpServer server;
    private final CrudService service;
    private final byte[] credentials;
    private final ObjectMapper json = JsonMapper.builder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).build();

    public DemoServer(String host, int port, String password) throws Exception {
        service = new CrudService(Database.factory());
        credentials = ("Basic " + Base64.getEncoder().encodeToString(("demo:" + password).getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", this::handle);
    }
    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!path.startsWith("/api/")) {
                if (!method.equals("GET")) { send(exchange, 405, Map.of("erro", "Metodo nao permitido.")); return; }
                String file = Map.of("/", "index.html", "/app.js", "app.js", "/style.css", "style.css", "/brand.png", "brand.png").get(path);
                if (file == null) { send(exchange, 404, Map.of("erro", "Pagina inexistente.")); return; }
                try (var input = DemoServer.class.getResourceAsStream("/web/" + file)) {
                    if (input == null) throw new IllegalStateException("Recurso web ausente.");
                    byte[] bytes = input.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", file.endsWith("png") ? "image/png" : file.endsWith("html") ? "text/html; charset=utf-8" : file.endsWith("js") ? "text/javascript; charset=utf-8" : "text/css; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
                }
                return;
            }
            String supplied = exchange.getRequestHeaders().getFirst("Authorization");
            if (supplied == null || !MessageDigest.isEqual(credentials, supplied.getBytes(StandardCharsets.UTF_8))) {
                send(exchange, 401, Map.of("erro", "Autentique com o usuario demo e a senha local.")); return;
            }
            String[] parts = path.substring(5).split("/", -1);
            if (parts.length < 1 || parts.length > 2) throw new CrudService.NotFound();
            String table = parts[0]; CrudService.table(table);
            UUID id = parts.length == 2 ? UUID.fromString(parts[1]) : null;
            if (method.equals("GET")) { send(exchange, 200, id == null ? service.list(table) : service.get(table, id)); return; }
            if (method.equals("DELETE") && id != null) { service.delete(table, id); send(exchange, 204, null); return; }
            if ((method.equals("POST") && id == null) || (method.equals("PUT") && id != null)) {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
                    send(exchange, 415, Map.of("erro", "Envie application/json.")); return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(8193);
                if (body.length > 8192) { send(exchange, 413, Map.of("erro", "Corpo excede 8 KiB.")); return; }
                var input = json.readValue(body, CrudService.Input.class);
                Object result = service.save(table, id, input);
                send(exchange, id == null ? 201 : 200, result); return;
            }
            send(exchange, 405, Map.of("erro", "Metodo nao permitido para esta rota."));
        } catch (CrudService.NotFound e) {
            send(exchange, 404, Map.of("erro", "Registro nao encontrado nesta organizacao."));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException e) {
            send(exchange, 400, Map.of("erro", e instanceof IllegalArgumentException ? e.getMessage() : "JSON invalido: confira campos e tipos."));
        } catch (Exception e) {
            SQLException sql = null;
            for (Throwable cause = e; cause != null; cause = cause.getCause()) if (cause instanceof SQLException s) sql = s;
            if (sql != null && "23505".equals(sql.getSQLState())) send(exchange, 409, Map.of("erro", "Já existe uma configuração com essa combinação de campos."));
            else if (sql != null && Set.of("23502", "23503", "23514", "22001").contains(sql.getSQLState())) send(exchange, 400, Map.of("erro", "Dados violam uma restricao do banco de demonstracao."));
            else { e.printStackTrace(); send(exchange, 500, Map.of("erro", "Falha interna. Consulte o log local da demo.")); }
        } finally { exchange.close(); }
    }
    private void send(HttpExchange exchange, int status, Object value) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (status == 204) { exchange.sendResponseHeaders(status, -1); return; }
        byte[] data = json.writeValueAsBytes(value);
        exchange.sendResponseHeaders(status, data.length); exchange.getResponseBody().write(data);
    }
    public static void main(String[] args) throws Exception {
        var app = new DemoServer(Database.env("DBF_BIND", "127.0.0.1"), Integer.parseInt(Database.env("DBF_HTTP_PORT", "8095")), Database.env("DBF_TOKEN", "demo-local-123"));
        Runtime.getRuntime().addShutdownHook(new Thread(app::close)); app.start();
        System.out.println("DEMO LOCAL: http://localhost:" + app.port() + " | usuario demo | senha DBF_TOKEN (padrao documentado)");
    }
}
