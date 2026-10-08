package br.com.caisora.contrato.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ContratoBuilderTest {
    @Test
    void deveReutilizarValidacaoDoConstrutor() {
        assertThatThrownBy(() -> Contrato.builder().construir())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Periodicidade obrigatoria");
    }

    @Test
    void devePreservarTodosOsDadosECriarObjetosIndependentes() {
        Organizacao organizacao = mock(Organizacao.class);
        String numero = "exemplo";
        Cliente cliente = mock(Cliente.class);
        Embarcacao embarcacao = mock(Embarcacao.class);
        TipoVaga tipoVagaContratada = TipoVaga.SECA;
        PeriodicidadeContrato periodicidade = PeriodicidadeContrato.MENSAL;
        LocalDate dataInicio = LocalDate.now();
        LocalDate dataFim = LocalDate.now().plusMonths(1);
        boolean renovacaoAutomatica = true;
        Integer diasAvisoPrevio = 10;
        BigDecimal valorBase = BigDecimal.TEN;
        Integer diaVencimento = 10;
        String observacoes = "exemplo";
        Usuario criadoPor = mock(Usuario.class);
        var builder = Contrato.builder()
            .organizacao(organizacao)
            .numero(numero)
            .cliente(cliente)
            .embarcacao(embarcacao)
            .tipoVagaContratada(tipoVagaContratada)
            .periodicidade(periodicidade)
            .dataInicio(dataInicio)
            .dataFim(dataFim)
            .renovacaoAutomatica(renovacaoAutomatica)
            .diasAvisoPrevio(diasAvisoPrevio)
            .valorBase(valorBase)
            .diaVencimento(diaVencimento)
            .observacoes(observacoes)
            .criadoPor(criadoPor);
        var primeiro = builder.construir();
        assertThat(primeiro.getOrganizacao()).isEqualTo(organizacao);
        assertThat(primeiro.getNumero()).isEqualTo(numero);
        assertThat(primeiro.getCliente()).isEqualTo(cliente);
        assertThat(primeiro.getEmbarcacao()).isEqualTo(embarcacao);
        assertThat(primeiro.getTipoVagaContratada()).isEqualTo(tipoVagaContratada);
        assertThat(primeiro.getPeriodicidade()).isEqualTo(periodicidade);
        assertThat(primeiro.getDataInicio()).isEqualTo(dataInicio);
        assertThat(primeiro.getDataFim()).isEqualTo(dataFim);
        assertThat(primeiro.isRenovacaoAutomatica()).isEqualTo(renovacaoAutomatica);
        assertThat(primeiro.getDiasAvisoPrevio()).isEqualTo(diasAvisoPrevio);
        assertThat(primeiro.getValorBase()).isEqualTo(valorBase);
        assertThat(primeiro.getDiaVencimento()).isEqualTo(diaVencimento);
        assertThat(primeiro.getObservacoes()).isEqualTo(observacoes);
        assertThat(primeiro.getCriadoPor()).isEqualTo(criadoPor);
        var segundo = builder.numero("outro").construir();
        assertThat(segundo).isNotSameAs(primeiro);
        assertThat(primeiro.getNumero()).isEqualTo(numero);
        assertThat(segundo.getNumero()).isEqualTo("outro");
    }
}
