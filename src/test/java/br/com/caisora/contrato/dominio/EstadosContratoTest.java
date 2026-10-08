package br.com.caisora.contrato.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EstadosContratoTest {
    // A tabela é uma especificação das transições permitidas, incluindo estados terminais.
    @ParameterizedTest(name = "{0} + {1} => {2}")
    @CsvSource({
            "RASCUNHO, enviarParaAssinatura, PENDENTE_ASSINATURA",
            "RASCUNHO, ativar, INVALIDA",
            "RASCUNHO, suspender, INVALIDA",
            "RASCUNHO, reativar, INVALIDA",
            "RASCUNHO, solicitarEncerramento, INVALIDA",
            "RASCUNHO, encerrar, INVALIDA",
            "RASCUNHO, cancelar, CANCELADO",
            "PENDENTE_ASSINATURA, enviarParaAssinatura, INVALIDA",
            "PENDENTE_ASSINATURA, ativar, ATIVO",
            "PENDENTE_ASSINATURA, suspender, INVALIDA",
            "PENDENTE_ASSINATURA, reativar, INVALIDA",
            "PENDENTE_ASSINATURA, solicitarEncerramento, INVALIDA",
            "PENDENTE_ASSINATURA, encerrar, INVALIDA",
            "PENDENTE_ASSINATURA, cancelar, CANCELADO",
            "ATIVO, enviarParaAssinatura, INVALIDA",
            "ATIVO, ativar, INVALIDA",
            "ATIVO, suspender, SUSPENSO",
            "ATIVO, reativar, INVALIDA",
            "ATIVO, solicitarEncerramento, EM_ENCERRAMENTO",
            "ATIVO, encerrar, INVALIDA",
            "ATIVO, cancelar, INVALIDA",
            "SUSPENSO, enviarParaAssinatura, INVALIDA",
            "SUSPENSO, ativar, INVALIDA",
            "SUSPENSO, suspender, INVALIDA",
            "SUSPENSO, reativar, ATIVO",
            "SUSPENSO, solicitarEncerramento, EM_ENCERRAMENTO",
            "SUSPENSO, encerrar, INVALIDA",
            "SUSPENSO, cancelar, INVALIDA",
            "EM_ENCERRAMENTO, enviarParaAssinatura, INVALIDA",
            "EM_ENCERRAMENTO, ativar, INVALIDA",
            "EM_ENCERRAMENTO, suspender, INVALIDA",
            "EM_ENCERRAMENTO, reativar, INVALIDA",
            "EM_ENCERRAMENTO, solicitarEncerramento, INVALIDA",
            "EM_ENCERRAMENTO, encerrar, ENCERRADO",
            "EM_ENCERRAMENTO, cancelar, INVALIDA",
            "ENCERRADO, enviarParaAssinatura, INVALIDA",
            "ENCERRADO, ativar, INVALIDA",
            "ENCERRADO, suspender, INVALIDA",
            "ENCERRADO, reativar, INVALIDA",
            "ENCERRADO, solicitarEncerramento, INVALIDA",
            "ENCERRADO, encerrar, INVALIDA",
            "ENCERRADO, cancelar, INVALIDA",
            "CANCELADO, enviarParaAssinatura, INVALIDA",
            "CANCELADO, ativar, INVALIDA",
            "CANCELADO, suspender, INVALIDA",
            "CANCELADO, reativar, INVALIDA",
            "CANCELADO, solicitarEncerramento, INVALIDA",
            "CANCELADO, encerrar, INVALIDA",
            "CANCELADO, cancelar, INVALIDA"
    })
    void deveRespeitarTransicoesAoRestaurarStatus(StatusContrato atual, String acao, String esperado)
            throws Exception {
        Contrato entidade = Contrato.builder().organizacao(mock(Organizacao.class)).numero("CTR-2026-000001")
            .cliente(mock(Cliente.class)).embarcacao(mock(Embarcacao.class))
            .tipoVagaContratada(TipoVaga.SECA).periodicidade(PeriodicidadeContrato.MENSAL)
            .dataInicio(LocalDate.now()).valorBase(BigDecimal.TEN).diaVencimento(10)
            .criadoPor(mock(Usuario.class)).construir();
        var campo = Contrato.class.getDeclaredField("status");
        campo.setAccessible(true);
        campo.set(entidade, atual);

        Runnable executar = switch (acao) {
            case "enviarParaAssinatura" -> () -> entidade.enviarParaAssinatura();
            case "ativar" -> () -> entidade.ativar(LocalDate.now());
            case "suspender" -> () -> entidade.suspender();
            case "reativar" -> () -> entidade.reativar();
            case "solicitarEncerramento" -> () -> entidade.solicitarEncerramento();
            case "encerrar" -> () -> entidade.encerrar(LocalDate.now());
            case "cancelar" -> () -> entidade.cancelar();
            default -> throw new IllegalArgumentException("Ação desconhecida");
        };

        if (esperado.equals("INVALIDA")) {
            assertThatThrownBy(executar::run).isInstanceOf(IllegalStateException.class);
            assertThat(entidade.getStatus()).isEqualTo(atual);
        } else {
            executar.run();
            assertThat(entidade.getStatus()).isEqualTo(StatusContrato.valueOf(esperado));
        }
    }
}
