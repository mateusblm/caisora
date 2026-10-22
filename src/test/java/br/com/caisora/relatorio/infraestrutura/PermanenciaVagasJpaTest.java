package br.com.caisora.relatorio.infraestrutura;

import static org.assertj.core.api.Assertions.*;
import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.relatorio.aplicacao.RelatorioService;
import br.com.caisora.relatorio.suporte.PermanenciaFixture;
import br.com.caisora.vaga.dominio.TipoVaga;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Import({TestcontainersConfiguration.class, DadosRelatorioJpa.class, RelatorioService.class})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PermanenciaVagasJpaTest {
    @Autowired EntityManager em;
    @Autowired DadosRelatorioJpa dados;
    @Autowired RelatorioService service;
    private PermanenciaFixture.Cenario cenario;
    private final LocalDate dia = LocalDate.of(2026, 10, 10);

    @BeforeEach void configurar() {
        cenario = PermanenciaFixture.criar(em);
        em.clear();
    }

    @Test void apenasEncerradasDentroDosLimitesInclusivosComDuracaoEfetivaCompleta() {
        var r = service.permanenciaVagas(cenario.marinaA().getId(), dia, dia, null, null);
        assertThat(r.resumo().quantidadeOcupacoes()).isEqualTo(3);
        assertThat(r.resumo().permanenciaMediaSegundos()).isEqualByComparingTo("7200");
        assertThat(r.resumo().menorPermanenciaSegundos()).isEqualByComparingTo("3600");
        assertThat(r.resumo().maiorPermanenciaSegundos()).isEqualByComparingTo("10800");
        assertThat(r.resumo().totalHorasOcupacao()).isEqualByComparingTo("6");
        assertThat(r.grupos()).hasSize(2);
    }

    @Test void combinaTipoSetorSemDistinguirCaixaEIncluiVagaHojeInativa() {
        var r = service.permanenciaVagas(cenario.marinaA().getId(), dia, dia, " norte ", TipoVaga.SECA);
        assertThat(r.resumo().quantidadeOcupacoes()).isEqualTo(2);
        assertThat(r.resumo().totalHorasOcupacao()).isEqualByComparingTo("4");
        assertThat(r.grupos()).hasSize(1);
        assertThat(r.grupos().getFirst().setor()).isEqualTo("Norte");
        assertThat(service.permanenciaVagas(cenario.marinaA().getId(), dia, dia, "Norte", TipoVaga.MOLHADA).grupos()).isEmpty();
    }

    @Test void separaOrganizacoesNoLadoDaOcupacaoEDaVaga() {
        var a = cenario.marinaA();
        var b = cenario.marinaB();
        assertThat(service.permanenciaVagas(b.getId(), dia, dia, null, null).resumo().totalHorasOcupacao()).isEqualByComparingTo("10");
        // Mesmo um vínculo inconsistente entre tenants deve ser excluído pelo join da vaga.
        em.createNativeQuery("""
                update ocupacoes set vaga_id = (select id from vagas where organizacao_id = :b limit 1)
                 where organizacao_id = :a and encerrada_em = :fim
                """).setParameter("b", b.getId()).setParameter("a", a.getId())
                .setParameter("fim", Instant.parse("2026-10-10T00:00:00Z")).executeUpdate();
        em.clear();
        assertThat(service.permanenciaVagas(a.getId(), dia, dia, null, null).resumo().quantidadeOcupacoes()).isEqualTo(2);
        assertThat(service.permanenciaVagas(b.getId(), dia, dia, null, null).resumo().quantidadeOcupacoes()).isEqualTo(1);
    }

    @Test void resultadoVazioNaoDividePorZeroNemAlteraRegistros() {
        long quantidadeAntes = em.createQuery("select count(o) from Ocupacao o", Long.class).getSingleResult();
        var r = service.permanenciaVagas(cenario.marinaA().getId(), dia.plusMonths(1), dia.plusMonths(1), null, null);
        assertThat(r.resumo().quantidadeOcupacoes()).isZero();
        assertThat(r.resumo().permanenciaMediaSegundos()).isNull();
        assertThat(r.resumo().menorPermanenciaSegundos()).isNull();
        assertThat(r.resumo().maiorPermanenciaSegundos()).isNull();
        assertThat(r.resumo().totalHorasOcupacao()).isZero();
        assertThat(r.grupos()).isEmpty();
        em.flush();
        assertThat(em.createQuery("select count(o) from Ocupacao o", Long.class).getSingleResult()).isEqualTo(quantidadeAntes);
    }
}
