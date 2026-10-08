package br.com.caisora.embarcacao.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EmbarcacaoBuilderTest {
    @Test
    void devePreservarTodosOsDadosECriarObjetosIndependentes() {
        Organizacao organizacao = mock(Organizacao.class);
        Cliente proprietario = mock(Cliente.class);
        String nome = "exemplo";
        TipoEmbarcacao tipo = TipoEmbarcacao.LANCHA;
        String fabricante = "exemplo";
        String modelo = "exemplo";
        Integer anoFabricacao = 10;
        String numeroInscricao = "exemplo";
        String numeroCasco = "exemplo";
        String portoInscricao = "exemplo";
        String codigoPaisBandeira = "exemplo";
        BigDecimal comprimentoTotalMetros = BigDecimal.TEN;
        BigDecimal bocaMetros = BigDecimal.TEN;
        BigDecimal caladoMetros = BigDecimal.TEN;
        BigDecimal pontalMetros = BigDecimal.TEN;
        BigDecimal alturaTotalMetros = BigDecimal.TEN;
        BigDecimal pesoKg = BigDecimal.TEN;
        Integer capacidadePessoas = 10;
        TipoPropulsao tipoPropulsao = TipoPropulsao.MOTOR;
        String corPredominante = "exemplo";
        String observacoes = "exemplo";
        var builder = Embarcacao.builder()
            .organizacao(organizacao)
            .proprietario(proprietario)
            .nome(nome)
            .tipo(tipo)
            .fabricante(fabricante)
            .modelo(modelo)
            .anoFabricacao(anoFabricacao)
            .numeroInscricao(numeroInscricao)
            .numeroCasco(numeroCasco)
            .portoInscricao(portoInscricao)
            .codigoPaisBandeira(codigoPaisBandeira)
            .comprimentoTotalMetros(comprimentoTotalMetros)
            .bocaMetros(bocaMetros)
            .caladoMetros(caladoMetros)
            .pontalMetros(pontalMetros)
            .alturaTotalMetros(alturaTotalMetros)
            .pesoKg(pesoKg)
            .capacidadePessoas(capacidadePessoas)
            .tipoPropulsao(tipoPropulsao)
            .corPredominante(corPredominante)
            .observacoes(observacoes);
        var primeiro = builder.construir();
        assertThat(primeiro.getOrganizacao()).isEqualTo(organizacao);
        assertThat(primeiro.getProprietario()).isEqualTo(proprietario);
        assertThat(primeiro.getNome()).isEqualTo(nome);
        assertThat(primeiro.getTipo()).isEqualTo(tipo);
        assertThat(primeiro.getFabricante()).isEqualTo(fabricante);
        assertThat(primeiro.getModelo()).isEqualTo(modelo);
        assertThat(primeiro.getAnoFabricacao()).isEqualTo(anoFabricacao);
        assertThat(primeiro.getNumeroInscricao()).isEqualTo(numeroInscricao);
        assertThat(primeiro.getNumeroCasco()).isEqualTo(numeroCasco);
        assertThat(primeiro.getPortoInscricao()).isEqualTo(portoInscricao);
        assertThat(primeiro.getCodigoPaisBandeira()).isEqualTo(codigoPaisBandeira);
        assertThat(primeiro.getComprimentoTotalMetros()).isEqualTo(comprimentoTotalMetros);
        assertThat(primeiro.getBocaMetros()).isEqualTo(bocaMetros);
        assertThat(primeiro.getCaladoMetros()).isEqualTo(caladoMetros);
        assertThat(primeiro.getPontalMetros()).isEqualTo(pontalMetros);
        assertThat(primeiro.getAlturaTotalMetros()).isEqualTo(alturaTotalMetros);
        assertThat(primeiro.getPesoKg()).isEqualTo(pesoKg);
        assertThat(primeiro.getCapacidadePessoas()).isEqualTo(capacidadePessoas);
        assertThat(primeiro.getTipoPropulsao()).isEqualTo(tipoPropulsao);
        assertThat(primeiro.getCorPredominante()).isEqualTo(corPredominante);
        assertThat(primeiro.getObservacoes()).isEqualTo(observacoes);
        var segundo = builder.nome("outro").construir();
        assertThat(segundo).isNotSameAs(primeiro);
        assertThat(primeiro.getNome()).isEqualTo(nome);
        assertThat(segundo.getNome()).isEqualTo("outro");
    }
}
