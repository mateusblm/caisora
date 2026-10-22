package br.com.caisora.relatorio.suporte;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;

/** Dados determinísticos exclusivamente para PostgreSQL descartável de testes. */
public final class PermanenciaFixture {
    public record Cenario(Organizacao marinaA, Organizacao marinaB) {}
    private PermanenciaFixture() {}

    public static Cenario criar(EntityManager em) {
        var a = organizacao(em, "se06-a");
        var b = organizacao(em, "se06-b");
        var barcoA = barco(em, a, "11111111111");
        var barcoB = barco(em, b, "22222222222");
        var norte = vaga(em, a, "N-01", TipoVaga.SECA, "Norte");
        var sul = vaga(em, a, "S-01", TipoVaga.MOLHADA, "Sul");
        var outraMarina = vaga(em, b, "N-01", TipoVaga.SECA, "Norte");
        encerrada(em, a, barcoA, norte, "2026-10-09T23:00:00Z", "2026-10-10T00:00:00Z");
        encerrada(em, a, barcoA, norte, "2026-10-10T20:59:59.999999Z", "2026-10-10T23:59:59.999999Z");
        encerrada(em, a, barcoA, sul, "2026-10-10T10:00:00Z", "2026-10-10T12:00:00Z");
        encerrada(em, a, barcoA, norte, "2026-10-10T20:00:00Z", "2026-10-11T00:00:00Z");
        encerrada(em, a, barcoA, norte, "2026-10-09T20:00:00Z", "2026-10-09T23:59:59.999999Z");
        encerrada(em, b, barcoB, outraMarina, "2026-10-10T00:00:00Z", "2026-10-10T10:00:00Z");
        // Uma ocupação ativa, mesmo com previsão dentro do período, nunca entra no histórico.
        em.persist(new Ocupacao(a, barcoA, sul, Instant.parse("2026-10-10T08:00:00Z"),
                Instant.parse("2026-10-10T16:00:00Z"), "Ativa: excluída do relatório histórico"));
        // A permanência histórica não depende de a vaga continuar ativa hoje.
        norte.inativar();
        em.flush();
        return new Cenario(a, b);
    }

    public static void encerrada(EntityManager em, Organizacao org, Embarcacao barco, Vaga vaga,
            String inicio, String fim) {
        var ocupacao = new Ocupacao(org, barco, vaga, Instant.parse(inicio),
                Instant.parse(fim).plusSeconds(3600), "Fixture SE06");
        ocupacao.encerrar(Instant.parse(fim));
        em.persist(ocupacao);
    }

    private static Organizacao organizacao(EntityManager em, String slug) {
        var org = Organizacao.criar("Marina " + slug, slug, null, null, slug + "@example.com", null);
        em.persist(org);
        return org;
    }

    private static Embarcacao barco(EntityManager em, Organizacao org, String documento) {
        var cliente = new Cliente(org, TipoPessoa.FISICA, "Cliente de demonstração SE06", null,
                documento, null, null, null, null);
        em.persist(cliente);
        var barco = Embarcacao.builder().organizacao(org).proprietario(cliente).nome("Barco SE06")
                .tipo(TipoEmbarcacao.LANCHA).codigoPaisBandeira("BR").numeroInscricao("SE06-" + documento)
                .comprimentoTotalMetros(BigDecimal.TEN).bocaMetros(BigDecimal.ONE)
                .tipoPropulsao(TipoPropulsao.MOTOR).construir();
        em.persist(barco);
        return barco;
    }

    private static Vaga vaga(EntityManager em, Organizacao org, String codigo, TipoVaga tipo, String setor) {
        var vaga = new Vaga(org, codigo, tipo, setor, null, BigDecimal.TEN, BigDecimal.TEN,
                null, null, null, false, false, null);
        em.persist(vaga);
        return vaga;
    }
}
