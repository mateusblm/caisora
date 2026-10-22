package br.com.caisora.referencia.visual;

import static org.assertj.core.api.Assertions.*;
import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import br.com.caisora.referencia.infraestrutura.ServidorReferencias;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "caisora.referencias.worldtides-habilitado=false",
        "caisora.referencias.worldtides-uso-autorizado=false",
        "caisora.referencias.worldtides-key="})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker=true)
@EnabledIfEnvironmentVariable(named="CAISORA_AIE_VISUAL",matches="true")
class ReferenciasVisualTest {
    private static ServidorReferencias servidor;
    @DynamicPropertySource static void provedores(DynamicPropertyRegistry props) throws Exception {
        servidor=new ServidorReferencias();
        servidor.corposPorRota.put("/api/v1/paises/-",ServidorReferencias.fixture("paises"));
        servidor.corposPorRota.put("/api/v1/localidades/estados/SC/municipios",ServidorReferencias.fixture("municipios"));
        servidor.corposPorRota.put("/api/feriados/v1/2026",ServidorReferencias.fixture("feriados"));
        props.add("caisora.referencias.ibge-url",servidor::url);
        props.add("caisora.referencias.brasilapi-url",servidor::url);
    }
    @AfterAll static void encerrar(){if(servidor!=null)servidor.close();}
    @LocalServerPort int porta;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired PasswordEncoder encoder;
    @Test void consultaCatalogosControladosComCapturasESemAlterarBandeiraOuCadastros() throws Exception {
        var tx=new TransactionTemplate(transacoes);
        var usuarios=tx.execute(s->AieVisualFixture.criar(em,encoder));
        var antes=tx.execute(s->contagens());
        AieVisualFixture.navegador(porta,"catalogos-controlados");
        tx.executeWithoutResult(s->{assertThat(em.find(Embarcacao.class,usuarios.embarcacaoId()).getCodigoPaisBandeira()).isEqualTo("PT");assertThat(contagens()).isEqualTo(antes);});
    }
    private java.util.Map<String,Long> contagens(){
        var r=new java.util.LinkedHashMap<String,Long>();
        for(String entidade:java.util.List.of("Cliente","Embarcacao","Vaga","Ocupacao","Contrato"))r.put(entidade,em.createQuery("select count(e) from "+entidade+" e",Long.class).getSingleResult());return r;
    }
}
