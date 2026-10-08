package br.com.caisora.contrato.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builder: monta contrato passo a passo, com nomes no lugar de uma lista longa de argumentos.
 * construir() cria um objeto novo e reutiliza as regras do construtor da entidade.
 * O builder não persiste dados nem substitui as validações do serviço.
 */
public class ContratoBuilder {
    private Organizacao organizacao;
    private String numero;
    private Cliente cliente;
    private Embarcacao embarcacao;
    private TipoVaga tipoVagaContratada;
    private PeriodicidadeContrato periodicidade;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private boolean renovacaoAutomatica;
    private Integer diasAvisoPrevio;
    private BigDecimal valorBase;
    private Integer diaVencimento;
    private String observacoes;
    private Usuario criadoPor;

    public ContratoBuilder organizacao(Organizacao organizacao) {
        this.organizacao = organizacao;
        return this;
    }

    public ContratoBuilder numero(String numero) {
        this.numero = numero;
        return this;
    }

    public ContratoBuilder cliente(Cliente cliente) {
        this.cliente = cliente;
        return this;
    }

    public ContratoBuilder embarcacao(Embarcacao embarcacao) {
        this.embarcacao = embarcacao;
        return this;
    }

    public ContratoBuilder tipoVagaContratada(TipoVaga tipoVagaContratada) {
        this.tipoVagaContratada = tipoVagaContratada;
        return this;
    }

    public ContratoBuilder periodicidade(PeriodicidadeContrato periodicidade) {
        this.periodicidade = periodicidade;
        return this;
    }

    public ContratoBuilder dataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
        return this;
    }

    public ContratoBuilder dataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
        return this;
    }

    public ContratoBuilder renovacaoAutomatica(boolean renovacaoAutomatica) {
        this.renovacaoAutomatica = renovacaoAutomatica;
        return this;
    }

    public ContratoBuilder diasAvisoPrevio(Integer diasAvisoPrevio) {
        this.diasAvisoPrevio = diasAvisoPrevio;
        return this;
    }

    public ContratoBuilder valorBase(BigDecimal valorBase) {
        this.valorBase = valorBase;
        return this;
    }

    public ContratoBuilder diaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
        return this;
    }

    public ContratoBuilder observacoes(String observacoes) {
        this.observacoes = observacoes;
        return this;
    }

    public ContratoBuilder criadoPor(Usuario criadoPor) {
        this.criadoPor = criadoPor;
        return this;
    }

    public Contrato construir() {
        return new Contrato(
            organizacao,
            numero,
            cliente,
            embarcacao,
            tipoVagaContratada,
            periodicidade,
            dataInicio,
            dataFim,
            renovacaoAutomatica,
            diasAvisoPrevio,
            valorBase,
            diaVencimento,
            observacoes,
            criadoPor
        );
    }
}
