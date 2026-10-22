package br.com.caisora.auditoria;

import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.relatorio.suporte.PermanenciaFixture;
import br.com.caisora.usuario.dominio.*;
import br.com.caisora.variabilidade.dominio.*;
import br.com.caisora.referencia.infraestrutura.ServidorReferencias;
import jakarta.persistence.EntityManager;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

/** Auditoria opt-in: nenhuma fixture ou endpoint deste arquivo existe em produção. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "caisora.referencias.worldtides-habilitado=false", "caisora.referencias.worldtides-uso-autorizado=false", "caisora.referencias.worldtides-key="})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker=true)
@EnabledIfEnvironmentVariable(named="CAISORA_APF_VISUAL",matches="true")
class AuditoriaVisualApfTest {
    private static ServidorReferencias provedor;
    @DynamicPropertySource static void provedores(DynamicPropertyRegistry p) throws Exception {
        provedor=new ServidorReferencias();
        // Somente Open-Meteo controlado; os demais grupos reutilizam evidências identificadas.
        var unidades="\"temperature_2m\":\"°C\",\"wind_speed_10m\":\"km/h\",\"wind_direction_10m\":\"°\",\"wind_gusts_10m\":\"km/h\",\"precipitation_probability\":\"%\",\"wave_height\":\"m\",\"wave_direction\":\"°\",\"wave_period\":\"s\",\"swell_wave_height\":\"m\",\"swell_wave_direction\":\"°\"";
        var valores="\"temperature_2m\":[24],\"wind_speed_10m\":[12],\"wind_direction_10m\":[90],\"wind_gusts_10m\":[18],\"precipitation_probability\":[30],\"wave_height\":[1.2],\"wave_direction\":[120],\"wave_period\":[8],\"swell_wave_height\":[0.8],\"swell_wave_direction\":[130]";
        String json="{\"hourly_units\":{"+unidades+"},\"hourly\":{\"time\":[\""+java.time.LocalDate.now(java.time.ZoneOffset.UTC)+"T12:00\"],"+valores+"}}";
        provedor.corposPorRota.put("/v1/forecast",json);provedor.corposPorRota.put("/v1/marine",json);
        p.add("caisora.open-meteo.weather-url",provedor::url);p.add("caisora.open-meteo.marine-url",provedor::url);
    }
    @AfterAll static void fechar(){if(provedor!=null)provedor.close();}
    @LocalServerPort int porta;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test void auditarJornadasComBancoDescartavel() throws Exception {
        new TransactionTemplate(transacoes).executeWithoutResult(s->{
            var c=PermanenciaFixture.criar(em);
            c.marinaA().configurarCoordenadas(new java.math.BigDecimal("-27.59"),new java.math.BigDecimal("-48.55"));
            for(var perfil:PerfilUsuario.values()) em.persist(Usuario.criar(c.marinaA(),"Auditoria "+perfil,
                perfil.name().toLowerCase()+"@example.com",encoder.encode("Auditoria-Teste-2026"),perfil));
            for(var m:ModuloSistema.values()) if(!m.isObrigatorio()) em.persist(ModuloOrganizacao.criar(c.marinaA().getId(),m,
                !Set.of(ModuloSistema.AMBIENTE,ModuloSistema.RELATORIOS,ModuloSistema.REFERENCIAS).contains(m)));
        });
        // Observador local de hashes, somente no processo de teste. Não retorna linhas ou segredos.
        var observador=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        observador.createContext("/snapshot",exchange->{
            try {
                var tabelas=jdbc.queryForList("select tablename from pg_tables where schemaname='public' and tablename <> 'flyway_schema_history' order by tablename",String.class);
                var mapa=new LinkedHashMap<String,Object>();
                for(String t:tabelas) mapa.put(t,jdbc.queryForMap("select count(*) as quantidade, md5(coalesce(string_agg(linha, '' order by linha),'')) as hash from (select row_to_json(x)::text as linha from \""+t+"\" x) dados"));
                byte[] corpo=json.writeValueAsBytes(mapa);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,corpo.length);exchange.getResponseBody().write(corpo);
            } catch(Exception e){exchange.sendResponseHeaders(500,-1);} finally {exchange.close();}
        });
        observador.start();
        var processo=new ProcessBuilder("node",Path.of("scripts/auditar-apf-browser.mjs").toAbsolutePath().toString(),"http://127.0.0.1:"+porta,
            "http://127.0.0.1:"+observador.getAddress().getPort()+"/snapshot").inheritIO().start();
        try {assertThat(processo.waitFor(20,TimeUnit.MINUTES)).isTrue();assertThat(processo.exitValue()).isZero();}
        finally {if(processo.isAlive())processo.destroyForcibly();observador.stop(0);}
    }
}
