package br.com.caisora.movimentacao.dominio;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.Vaga;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegrasMovimentacaoTest {
    @ParameterizedTest(name = "{0}: {1} -> {2}, permitido={3}")
    @CsvSource({
        "LANCAMENTO, VAGA, AGUA, true",
        "LANCAMENTO, VAGA, PIER_ESPERA, true",
        "LANCAMENTO, AGUA, PIER_ESPERA, false",
        "LANCAMENTO, VAGA, AREA_SERVICO, false",
        "RETIRADA, AGUA, VAGA, true",
        "RETIRADA, PIER_ESPERA, VAGA, true",
        "RETIRADA, AREA_SERVICO, VAGA, false",
        "RETIRADA, AGUA, AREA_SERVICO, false",
        "RETORNO_PARA_VAGA, AREA_SERVICO, VAGA, true",
        "RETORNO_PARA_VAGA, EXTERNA, VAGA, true",
        "RETORNO_PARA_VAGA, AGUA, VAGA, false",
        "RETORNO_PARA_VAGA, AREA_SERVICO, PIER_ESPERA, false",
        "TRANSFERENCIA, VAGA, VAGA, true",
        "TRANSFERENCIA, AGUA, VAGA, false",
        "TRANSFERENCIA, VAGA, PIER_ESPERA, false",
        "DESLOCAMENTO_INTERNO, VAGA, PIER_ESPERA, true",
        "DESLOCAMENTO_INTERNO, VAGA, AREA_SERVICO, true",
        "DESLOCAMENTO_INTERNO, AREA_SERVICO, EXTERNA, true",
        "DESLOCAMENTO_INTERNO, VAGA, AGUA, false",
        "DESLOCAMENTO_INTERNO, AREA_SERVICO, VAGA, false",
        "DESLOCAMENTO_INTERNO, EXTERNA, DESCONHECIDA, false"
    })
    void deveValidarPercursoNaCriacaoDaEntidade(TipoMovimentacao tipo,
            TipoPosicaoEmbarcacao origem, TipoPosicaoEmbarcacao destino, boolean permitido) {
        Runnable criar = () -> new Movimentacao(
            mock(Organizacao.class), mock(Embarcacao.class), tipo,
            PrioridadeMovimentacao.NORMAL, origem, vagaQuandoNecessaria(origem), null,
            destino, vagaQuandoNecessaria(destino), null, Instant.now(),
            mock(Usuario.class), null, null
        );
        if (permitido) {
            assertThatCode(criar::run).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(criar::run).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private Vaga vagaQuandoNecessaria(TipoPosicaoEmbarcacao posicao) {
        if (posicao != TipoPosicaoEmbarcacao.VAGA) {
            return null;
        }
        Vaga vaga = mock(Vaga.class);
        when(vaga.getId()).thenReturn(UUID.randomUUID());
        return vaga;
    }
}
