package br.com.caisora.referencia.infraestrutura;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Servidor HTTP controlado, exclusivamente no classpath de testes. */
public final class ServidorReferencias implements AutoCloseable {
    private final HttpServer server;
    public volatile String corpo="[]", ultimaUri="";
    public volatile int status=200;
    public volatile long atraso=0;
    public volatile boolean gzip=false;
    public final java.util.Map<String,String> corposPorRota=new java.util.concurrent.ConcurrentHashMap<>();
    public final AtomicInteger chamadas=new AtomicInteger();
    public ServidorReferencias() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            chamadas.incrementAndGet();ultimaUri=exchange.getRequestURI().toString();
            try {
                if(atraso>0) Thread.sleep(atraso);
                byte[] bytes=corposPorRota.getOrDefault(exchange.getRequestURI().getPath(),corpo).getBytes(StandardCharsets.UTF_8);
                if(gzip){var out=new java.io.ByteArrayOutputStream();try(var stream=new java.util.zip.GZIPOutputStream(out)){stream.write(bytes);}bytes=out.toByteArray();exchange.getResponseHeaders().add("Content-Encoding","gzip");}
                exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
            }catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}
        });server.start();
    }
    public String url(){return "http://127.0.0.1:"+server.getAddress().getPort();}
    public static String fixture(String nome) throws Exception {
        try(var in=ServidorReferencias.class.getResourceAsStream("/referencias/"+nome+".json")){
            if(in==null)throw new IllegalArgumentException("Fixture ausente");return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Override public void close(){server.stop(0);}
}
