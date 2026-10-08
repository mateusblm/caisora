package br.com.caisora.embarcacao.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.organizacao.dominio.Organizacao;
import java.math.BigDecimal;

/**
 * Builder: monta embarcacao passo a passo, com nomes no lugar de uma lista longa de argumentos.
 * construir() cria um objeto novo e reutiliza as regras do construtor da entidade.
 * O builder não persiste dados nem substitui as validações do serviço.
 */
public class EmbarcacaoBuilder {
    private Organizacao organizacao;
    private Cliente proprietario;
    private String nome;
    private TipoEmbarcacao tipo;
    private String fabricante;
    private String modelo;
    private Integer anoFabricacao;
    private String numeroInscricao;
    private String numeroCasco;
    private String portoInscricao;
    private String codigoPaisBandeira;
    private BigDecimal comprimentoTotalMetros;
    private BigDecimal bocaMetros;
    private BigDecimal caladoMetros;
    private BigDecimal pontalMetros;
    private BigDecimal alturaTotalMetros;
    private BigDecimal pesoKg;
    private Integer capacidadePessoas;
    private TipoPropulsao tipoPropulsao;
    private String corPredominante;
    private String observacoes;

    public EmbarcacaoBuilder organizacao(Organizacao organizacao) {
        this.organizacao = organizacao;
        return this;
    }

    public EmbarcacaoBuilder proprietario(Cliente proprietario) {
        this.proprietario = proprietario;
        return this;
    }

    public EmbarcacaoBuilder nome(String nome) {
        this.nome = nome;
        return this;
    }

    public EmbarcacaoBuilder tipo(TipoEmbarcacao tipo) {
        this.tipo = tipo;
        return this;
    }

    public EmbarcacaoBuilder fabricante(String fabricante) {
        this.fabricante = fabricante;
        return this;
    }

    public EmbarcacaoBuilder modelo(String modelo) {
        this.modelo = modelo;
        return this;
    }

    public EmbarcacaoBuilder anoFabricacao(Integer anoFabricacao) {
        this.anoFabricacao = anoFabricacao;
        return this;
    }

    public EmbarcacaoBuilder numeroInscricao(String numeroInscricao) {
        this.numeroInscricao = numeroInscricao;
        return this;
    }

    public EmbarcacaoBuilder numeroCasco(String numeroCasco) {
        this.numeroCasco = numeroCasco;
        return this;
    }

    public EmbarcacaoBuilder portoInscricao(String portoInscricao) {
        this.portoInscricao = portoInscricao;
        return this;
    }

    public EmbarcacaoBuilder codigoPaisBandeira(String codigoPaisBandeira) {
        this.codigoPaisBandeira = codigoPaisBandeira;
        return this;
    }

    public EmbarcacaoBuilder comprimentoTotalMetros(BigDecimal comprimentoTotalMetros) {
        this.comprimentoTotalMetros = comprimentoTotalMetros;
        return this;
    }

    public EmbarcacaoBuilder bocaMetros(BigDecimal bocaMetros) {
        this.bocaMetros = bocaMetros;
        return this;
    }

    public EmbarcacaoBuilder caladoMetros(BigDecimal caladoMetros) {
        this.caladoMetros = caladoMetros;
        return this;
    }

    public EmbarcacaoBuilder pontalMetros(BigDecimal pontalMetros) {
        this.pontalMetros = pontalMetros;
        return this;
    }

    public EmbarcacaoBuilder alturaTotalMetros(BigDecimal alturaTotalMetros) {
        this.alturaTotalMetros = alturaTotalMetros;
        return this;
    }

    public EmbarcacaoBuilder pesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
        return this;
    }

    public EmbarcacaoBuilder capacidadePessoas(Integer capacidadePessoas) {
        this.capacidadePessoas = capacidadePessoas;
        return this;
    }

    public EmbarcacaoBuilder tipoPropulsao(TipoPropulsao tipoPropulsao) {
        this.tipoPropulsao = tipoPropulsao;
        return this;
    }

    public EmbarcacaoBuilder corPredominante(String corPredominante) {
        this.corPredominante = corPredominante;
        return this;
    }

    public EmbarcacaoBuilder observacoes(String observacoes) {
        this.observacoes = observacoes;
        return this;
    }

    public Embarcacao construir() {
        return new Embarcacao(
            organizacao,
            proprietario,
            nome,
            tipo,
            fabricante,
            modelo,
            anoFabricacao,
            numeroInscricao,
            numeroCasco,
            portoInscricao,
            codigoPaisBandeira,
            comprimentoTotalMetros,
            bocaMetros,
            caladoMetros,
            pontalMetros,
            alturaTotalMetros,
            pesoKg,
            capacidadePessoas,
            tipoPropulsao,
            corPredominante,
            observacoes
        );
    }
}
