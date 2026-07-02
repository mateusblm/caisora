package br.com.caisora.contrato.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import org.junit.jupiter.api.Test;

class VinculoContratoOcupacaoTest {

    @Test
    void deveFinalizarVinculoAberto() {
        VinculoContratoOcupacao vinculo =
            new VinculoContratoOcupacao(
                mock(Organizacao.class),
                mock(Contrato.class),
                mock(Ocupacao.class)
            );

        vinculo.finalizar("Mudanca de vaga");

        assertThat(vinculo.estaAberto()).isFalse();
        assertThat(vinculo.getFimEm()).isNotNull();
        assertThat(vinculo.getMotivoFim())
            .isEqualTo("Mudanca de vaga");
    }

    @Test
    void naoDeveFinalizarDuasVezes() {
        VinculoContratoOcupacao vinculo =
            new VinculoContratoOcupacao(
                mock(Organizacao.class),
                mock(Contrato.class),
                mock(Ocupacao.class)
            );
        vinculo.finalizar("Primeiro encerramento");

        assertThatThrownBy(() -> vinculo.finalizar("Novamente"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Vinculo de ocupacao ja finalizado");
    }
}
