package br.com.caisora.relatorio.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.relatorio.dominio.DadosRelatorio;
import br.com.caisora.relatorio.dominio.DadosRelatorio.PermanenciaOcupacao;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PermanenciaVagasServiceTest {
    private final DadosRelatorio dados = mock(DadosRelatorio.class);
    private final RelatorioService service = new RelatorioService(dados);
    private final UUID org = UUID.randomUUID();
    private final LocalDate dia = LocalDate.of(2026, 10, 10);

    @Test void calculaMediaMinimoMaximoHorasEGruposPonderandoPorOcupacao() {
        when(dados.permanencias(any(), any(), any(), any(), any())).thenReturn(List.of(
                ocupacao("Norte", TipoVaga.SECA, 3600), ocupacao("Norte", TipoVaga.SECA, 10800),
                ocupacao("Sul", TipoVaga.MOLHADA, 18000)));
        var r = service.permanenciaVagas(org, dia, dia, null, null);
        assertThat(r.resumo().quantidadeOcupacoes()).isEqualTo(3);
        assertThat(r.resumo().permanenciaMediaSegundos()).isEqualByComparingTo("10800");
        assertThat(r.resumo().menorPermanenciaSegundos()).isEqualByComparingTo("3600");
        assertThat(r.resumo().maiorPermanenciaSegundos()).isEqualByComparingTo("18000");
        assertThat(r.resumo().totalHorasOcupacao()).isEqualByComparingTo("9");
        assertThat(r.grupos()).hasSize(2);
        assertThat(r.grupos().getFirst().indicadores().quantidadeOcupacoes()).isEqualTo(2);
        assertThat(r.grupos().getFirst().indicadores().permanenciaMediaSegundos()).isEqualByComparingTo("7200");
        assertThat(r.grupos().getFirst().indicadores().totalHorasOcupacao()).isEqualByComparingTo("4");
    }

    @Test void passaTipoSetorNormalizadoOrganizacaoELimitesUTCParaAPorta() {
        when(dados.permanencias(any(), any(), any(), any(), any())).thenReturn(List.of());
        service.permanenciaVagas(org, dia, dia.plusDays(1), " Norte ", TipoVaga.SECA);
        verify(dados).permanencias(org, Instant.parse("2026-10-10T00:00:00Z"),
                Instant.parse("2026-10-12T00:00:00Z"), "Norte", TipoVaga.SECA);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", "  "})
    void intervaloVazioNaoInventaMediaNemExtremos(String setor) {
        when(dados.permanencias(any(), any(), any(), any(), any())).thenReturn(List.of());
        var r = service.permanenciaVagas(org, dia, dia, setor, null);
        assertThat(r.resumo().quantidadeOcupacoes()).isZero();
        assertThat(r.resumo().totalHorasOcupacao()).isZero();
        assertThat(r.resumo().permanenciaMediaSegundos()).isNull();
        assertThat(r.resumo().menorPermanenciaSegundos()).isNull();
        assertThat(r.resumo().maiorPermanenciaSegundos()).isNull();
        assertThat(r.grupos()).isEmpty();
        verify(dados).permanencias(eq(org), any(), any(), isNull(), isNull());
    }

    @Test void duracaoZeroValidaESetorAusenteNaoSaoConfundidosComFaltaDeDados() {
        when(dados.permanencias(any(), any(), any(), any(), any()))
                .thenReturn(List.of(ocupacao(null, TipoVaga.POITA, 0)));
        var r = service.permanenciaVagas(org, dia, dia, null, null);
        assertThat(r.resumo().quantidadeOcupacoes()).isEqualTo(1);
        assertThat(r.resumo().permanenciaMediaSegundos()).isZero();
        assertThat(r.resumo().menorPermanenciaSegundos()).isZero();
        assertThat(r.grupos().getFirst().setor()).isNull();
    }

    @Test void preservaFracoesAntesDoArredondamentoDaMedia() {
        Instant inicio = Instant.parse("2026-10-09T00:00:00Z");
        when(dados.permanencias(any(), any(), any(), any(), any())).thenReturn(List.of(
                new PermanenciaOcupacao(UUID.randomUUID(), "Norte", TipoVaga.SECA, inicio, inicio.plusNanos(1_005_000_000L)),
                new PermanenciaOcupacao(UUID.randomUUID(), "Norte", TipoVaga.SECA, inicio, inicio.plusNanos(1_004_000_000L))));
        assertThat(service.permanenciaVagas(org, dia, dia, null, null).resumo().permanenciaMediaSegundos())
                .isEqualByComparingTo("1.00");
    }

    @Test void rejeitaCronologiaDeFiltroCamposObrigatoriosELimitesAntesDaPesquisa() {
        assertThatThrownBy(() -> service.permanenciaVagas(org, null, dia, null, null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.permanenciaVagas(org, dia, null, null, null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.permanenciaVagas(org, dia.plusDays(1), dia, null, null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.permanenciaVagas(org, dia, LocalDate.MAX, null, null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.permanenciaVagas(org, LocalDate.of(0, 1, 1), dia, null, null)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.permanenciaVagas(org, dia, dia, "x".repeat(101), null)).isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(dados);
    }

    @Test void registrosHistoricosIncompletosOuInvertidosGeramErroExplicito() {
        var instante = Instant.parse("2026-10-10T00:00:00Z");
        for (var registro : List.of(
                new PermanenciaOcupacao(UUID.randomUUID(), null, TipoVaga.SECA, null, instante),
                new PermanenciaOcupacao(UUID.randomUUID(), null, TipoVaga.SECA, instante, null),
                new PermanenciaOcupacao(UUID.randomUUID(), null, TipoVaga.SECA, instante, instante.minusSeconds(1)),
                new PermanenciaOcupacao(UUID.randomUUID(), null, null, instante, instante))) {
            when(dados.permanencias(any(), any(), any(), any(), any())).thenReturn(List.of(registro));
            assertThatThrownBy(() -> service.permanenciaVagas(org, dia, dia, null, null))
                    .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("cronologia inválida");
        }
    }

    private PermanenciaOcupacao ocupacao(String setor, TipoVaga tipo, long segundos) {
        var fim = Instant.parse("2026-10-10T12:00:00Z");
        return new PermanenciaOcupacao(UUID.randomUUID(), setor, tipo, fim.minusSeconds(segundos), fim);
    }
}
