package br.com.caisora.movimentacao.dominio;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.Vaga;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EstadosMovimentacaoTest {
    // A tabela é uma especificação das transições permitidas, incluindo estados terminais.
    @ParameterizedTest(name = "{0} + {1} => {2}")
    @CsvSource({
            "AGENDADA, iniciar, EM_EXECUCAO",
            "AGENDADA, concluir, INVALIDA",
            "AGENDADA, cancelar, CANCELADA",
            "EM_EXECUCAO, iniciar, INVALIDA",
            "EM_EXECUCAO, concluir, CONCLUIDA",
            "EM_EXECUCAO, cancelar, INVALIDA",
            "CONCLUIDA, iniciar, INVALIDA",
            "CONCLUIDA, concluir, INVALIDA",
            "CONCLUIDA, cancelar, INVALIDA",
            "CANCELADA, iniciar, INVALIDA",
            "CANCELADA, concluir, INVALIDA",
            "CANCELADA, cancelar, INVALIDA"
    })
    void deveRespeitarTransicoesAoRestaurarStatus(StatusMovimentacao atual, String acao, String esperado)
            throws Exception {
        Movimentacao entidade = new Movimentacao(mock(Organizacao.class), mock(Embarcacao.class),
            TipoMovimentacao.LANCAMENTO, PrioridadeMovimentacao.NORMAL,
            TipoPosicaoEmbarcacao.VAGA, mock(Vaga.class), null,
            TipoPosicaoEmbarcacao.AGUA, null, null, Instant.now(),
            mock(Usuario.class), null, null);
        var campo = Movimentacao.class.getDeclaredField("status");
        campo.setAccessible(true);
        campo.set(entidade, atual);
        var inicio = Movimentacao.class.getDeclaredField("iniciadaEm");
        inicio.setAccessible(true);
        inicio.set(entidade, Instant.now().minusSeconds(60));

        Runnable executar = switch (acao) {
            case "iniciar" -> () -> entidade.iniciar(mock(Usuario.class), Instant.now());
            case "concluir" -> () -> entidade.concluir(mock(Usuario.class), Instant.now());
            case "cancelar" -> () -> entidade.cancelar(Instant.now(), "Solicitação do cliente");
            default -> throw new IllegalArgumentException("Ação desconhecida");
        };

        if (esperado.equals("INVALIDA")) {
            assertThatThrownBy(executar::run).isInstanceOf(IllegalStateException.class);
            assertThat(entidade.getStatus()).isEqualTo(atual);
        } else {
            executar.run();
            assertThat(entidade.getStatus()).isEqualTo(StatusMovimentacao.valueOf(esperado));
        }
    }
}
