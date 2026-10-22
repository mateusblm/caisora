package br.com.caisora.relatorio.visual;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.relatorio.suporte.PermanenciaFixture;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.variabilidade.dominio.ModuloOrganizacao;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Demonstração opt-in: sobe a API real e popula somente o container PostgreSQL descartável. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "CAISORA_VISUAL", matches = "true")
class PermanenciaVagasVisualTest {
    @LocalServerPort int porta;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired PasswordEncoder encoder;

    @Test void capturaDesktopEMobileComApiReal() throws Exception {
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            var cenario = PermanenciaFixture.criar(em);
            em.persist(Usuario.criar(cenario.marinaA(), "Administrador SE06",
                    "demo-se06@example.com", encoder.encode("Demo-SE06-2026"), PerfilUsuario.ADMINISTRADOR_MARINA));
            em.persist(ModuloOrganizacao.criar(cenario.marinaA().getId(), ModuloSistema.RELATORIOS, true));
        });
        var processo = new ProcessBuilder("node", Path.of("scripts/validar-se06-browser.mjs").toAbsolutePath().toString(),
                "http://127.0.0.1:" + porta).inheritIO().start();
        try {
            assertThat(processo.waitFor(120, TimeUnit.SECONDS)).as("inspeção real no Chromium").isTrue();
            assertThat(processo.exitValue()).isZero();
        } finally {
            if (processo.isAlive()) processo.destroyForcibly();
        }
    }
}
