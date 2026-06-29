package br.com.caisora.organizacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlugOrganizacaoTest {

    @Test
    void deveNormalizarCodigoDaMarina() {
        assertThat(SlugOrganizacao.normalizar(" MARINA-TESTE "))
                .isEqualTo("marina-teste");
    }

    @Test
    void deveRemoverAcentosAoNormalizar() {
        assertThat(SlugOrganizacao.normalizar(" Marina-Náutica2 "))
                .isEqualTo("marina-nautica2");
    }

    @Test
    void deveRejeitarSlugVazio() {
        assertThatThrownBy(() -> SlugOrganizacao.normalizar(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Codigo da marina e obrigatorio");
    }

    @Test
    void deveRejeitarFormatoInvalido() {
        assertThatThrownBy(() -> SlugOrganizacao.normalizar("marina__teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Codigo da marina deve conter letras, numeros e hifens simples");
    }
}
