package br.com.caisora.ambiente.infraestrutura.openmeteo;
import static org.assertj.core.api.Assertions.*;
import br.com.caisora.ambiente.aplicacao.ProvedorIndisponivelException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
class OpenMeteoClientTest {
 private HttpServer server;private OpenMeteoClient client;private final AtomicReference<String> query=new AtomicReference<>();
 private String body;private int status=200;private long delay=0;
 @BeforeEach void iniciar() throws Exception {
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/", exchange->{query.set(exchange.getRequestURI().toString());
   try {if(delay>0)Thread.sleep(delay);var bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}
   catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}});
  server.start();var url="http://127.0.0.1:"+server.getAddress().getPort();client=new OpenMeteoClient(url,url,"chave-controlada",100,100);
 }
 @AfterEach void parar(){server.stop(0);}
 @Test void consultaMeteorologiaComParametrosEUnidades() {
  body="""
   {"hourly_units":{"temperature_2m":"°C","wind_speed_10m":"km/h","wind_direction_10m":"°","wind_gusts_10m":"km/h","precipitation_probability":"%"},
    "hourly":{"time":["2026-10-22T00:00"],"temperature_2m":[22],"wind_speed_10m":[10],"wind_direction_10m":[180],"wind_gusts_10m":[20],"precipitation_probability":[40]}}
   """;
  var r=consultar(false);assertThat(r.incompleta()).isFalse();assertThat(r.horas().getFirst().valores().get("temperature_2m")).isEqualTo(22);
  assertThat(r.horas().getFirst().horario()).endsWith("Z");assertThat(r.unidades().get("wind_speed_10m")).isEqualTo("km/h");
  assertThat(query.get()).contains("/v1/forecast?","latitude=-25","longitude=-48","start_date=2026-10-22","end_date=2026-10-22","timezone=UTC","apikey=chave-controlada");
 }
 @Test void dadosMaritimosAusentesNaoViramZero() {
  body="""
   {"hourly_units":{"wave_height":"m"},"hourly":{"time":["2026-10-22T00:00","2026-10-22T01:00"],"wave_height":[1.5,null],"wave_period":[7]}}
   """;
  var r=consultar(true);assertThat(r.incompleta()).isTrue();assertThat(r.horas()).hasSize(2);
  assertThat(r.horas().get(1).valores().get("wave_height")).isNull();assertThat(r.horas().getFirst().valores().get("swell_wave_direction")).isNull();assertThat(query.get()).contains("/v1/marine?");
 }
 @Test void trataIndisponibilidade(){body="{}";status=503;assertThatThrownBy(()->consultar(false)).isInstanceOf(ProvedorIndisponivelException.class);}
 @Test void trataTimeout(){body="{}";delay=300;assertThatThrownBy(()->consultar(true)).isInstanceOf(ProvedorIndisponivelException.class);}
 @Test void trataJsonInvalido(){body="nao-json";assertThatThrownBy(()->consultar(false)).isInstanceOf(ProvedorIndisponivelException.class);}
 @Test void trataEstruturaInvalida(){body="{\"hourly\":{\"time\":null}}";assertThatThrownBy(()->consultar(false)).isInstanceOf(ProvedorIndisponivelException.class);}
 @Test void intervaloSemDadosEIdentificado(){body="{\"hourly\":{\"time\":[]}}";var r=consultar(false);assertThat(r.horas()).isEmpty();assertThat(r.incompleta()).isTrue();}
 private br.com.caisora.ambiente.api.PrevisaoResponse consultar(boolean mar){return client.consultar(mar,-25,-48,LocalDate.of(2026,10,22),LocalDate.of(2026,10,22));}
}
