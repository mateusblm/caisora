package br.com.caisora.referencia.visual;

import static org.assertj.core.api.Assertions.*;
import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.referencia.infraestrutura.ServidorReferencias;
import br.com.caisora.autenticacao.aplicacao.GeradorTokenJwt;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "caisora.referencias.worldtides-habilitado=true",
        "caisora.referencias.worldtides-uso-autorizado=true",
        "caisora.referencias.worldtides-key=chave-exclusiva-de-teste"})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker=true)
@EnabledIfEnvironmentVariable(named="CAISORA_AIE_VISUAL",matches="true")
class MaresControladasVisualTest {
    private static ServidorReferencias servidor;
    @DynamicPropertySource static void provedor(DynamicPropertyRegistry props) throws Exception {
        servidor=new ServidorReferencias();servidor.corpo=ServidorReferencias.fixture("worldtides");
        props.add("caisora.referencias.worldtides-url",servidor::url);
    }
    @AfterAll static void fechar(){if(servidor!=null)servidor.close();}
    @LocalServerPort int porta;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired PasswordEncoder encoder;
    @Autowired GeradorTokenJwt tokens;
    @Test void marésControladasComIsolamentoDeCoordenadasSemUsoDaApiPaga() throws Exception {
        var usuarios=new TransactionTemplate(transacoes).execute(s->AieVisualFixture.criar(em,encoder));
        AieVisualFixture.navegador(porta,"mares-controladas");
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/referencias/mares?inicio="+LocalDate.now(java.time.ZoneOffset.UTC)))
                .header("Authorization","Bearer "+tokens.gerarToken(usuarios.b())).GET().build();
        assertThat(HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(servidor.ultimaUri).contains("lat=-23.0","lon=-43.0").doesNotContain("lat=-27.59");
    }
}
