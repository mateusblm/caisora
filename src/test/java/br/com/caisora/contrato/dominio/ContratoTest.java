package br.com.caisora.contrato.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ContratoTest {

    @Test
    void deveExecutarFluxoPrincipalAteEncerramento() {
        Contrato contrato = criarContrato();

        contrato.enviarParaAssinatura();
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.PENDENTE_ASSINATURA);

        contrato.ativar(LocalDate.now());
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.ATIVO);

        contrato.solicitarEncerramento();
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.EM_ENCERRAMENTO);

        contrato.encerrar(LocalDate.now());
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.ENCERRADO);
        assertThat(contrato.getDataEncerramento())
            .isEqualTo(LocalDate.now());
    }

    @Test
    void deveSuspenderEReativarContrato() {
        Contrato contrato = criarContrato();
        contrato.enviarParaAssinatura();
        contrato.ativar(LocalDate.now());

        contrato.suspender();
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.SUSPENSO);

        contrato.reativar();
        assertThat(contrato.getStatus())
            .isEqualTo(StatusContrato.ATIVO);
    }

    @Test
    void naoDeveEditarContratoForaDoRascunho() {
        Contrato contrato = criarContrato();
        contrato.enviarParaAssinatura();

        assertThatThrownBy(() -> contrato.atualizarDados(
            mock(Cliente.class),
            mock(Embarcacao.class),
            TipoVaga.SECA,
            PeriodicidadeContrato.MENSAL,
            LocalDate.now(),
            null,
            false,
            30,
            new BigDecimal("1000.00"),
            10,
            null
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Operacao invalida");
    }

    @Test
    void deveExigirDataFimNaPeriodicidadePersonalizada() {
        assertThatThrownBy(() -> new Contrato(
            mock(Organizacao.class),
            "CTR-2026-000001",
            mock(Cliente.class),
            mock(Embarcacao.class),
            TipoVaga.SECA,
            PeriodicidadeContrato.PERSONALIZADA,
            LocalDate.now(),
            null,
            false,
            30,
            new BigDecimal("1000.00"),
            10,
            null,
            mock(Usuario.class)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Data de fim obrigatoria");
    }

    @Test
    void assinaturaInvalidaNaoDeveAlterarEstadoOuDatas() {
        Contrato contrato = criarContrato();
        contrato.enviarParaAssinatura();

        assertThatThrownBy(() -> contrato.ativar(LocalDate.now().plusDays(1)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.PENDENTE_ASSINATURA);
        assertThat(contrato.getDataAssinatura()).isNull();
        assertThat(contrato.getDataAtivacao()).isNull();
    }

    private Contrato criarContrato() {
        return new Contrato(
            mock(Organizacao.class),
            "CTR-2026-000001",
            mock(Cliente.class),
            mock(Embarcacao.class),
            TipoVaga.SECA,
            PeriodicidadeContrato.MENSAL,
            LocalDate.now(),
            null,
            true,
            30,
            new BigDecimal("1500.00"),
            10,
            "Contrato mensal",
            mock(Usuario.class)
        );
    }
}
