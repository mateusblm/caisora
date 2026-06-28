package br.com.caisora.embarcacao.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.organizacao.dominio.Organizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "embarcacoes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_embarcacao_organizacao_numero_inscricao",
                        columnNames = {
                                "organizacao_id",
                                "numero_inscricao"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_embarcacao_organizacao_numero_casco",
                        columnNames = {
                                "organizacao_id",
                                "numero_casco"
                        }
                )
        }
)
public class Embarcacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Cliente proprietario;

    @Column(length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEmbarcacao tipo;

    @Column(length = 100)
    private String fabricante;

    @Column(length = 100)
    private String modelo;

    @Column(name = "ano_fabricacao")
    private Integer anoFabricacao;

    @Column(name = "numero_inscricao", length = 50)
    private String numeroInscricao;

    @Column(name = "numero_casco", length = 100)
    private String numeroCasco;

    @Column(name = "porto_inscricao", length = 150)
    private String portoInscricao;

    @Column(
            name = "codigo_pais_bandeira",
            nullable = false,
            length = 2
    )
    private String codigoPaisBandeira;

    @Column(
            name = "comprimento_total_metros",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal comprimentoTotalMetros;

    @Column(
            name = "boca_metros",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal bocaMetros;

    @Column(
            name = "calado_metros",
            precision = 8,
            scale = 2
    )
    private BigDecimal caladoMetros;

    @Column(
            name = "pontal_metros",
            precision = 8,
            scale = 2
    )
    private BigDecimal pontalMetros;

    @Column(
            name = "altura_total_metros",
            precision = 8,
            scale = 2
    )
    private BigDecimal alturaTotalMetros;

    @Column(
            name = "peso_kg",
            precision = 12,
            scale = 2
    )
    private BigDecimal pesoKg;

    @Column(name = "capacidade_pessoas")
    private Integer capacidadePessoas;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_propulsao",
            nullable = false,
            length = 30
    )
    private TipoPropulsao tipoPropulsao;

    @Column(name = "cor_predominante", length = 50)
    private String corPredominante;

    @Column(length = 2000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativa;

    @Column(
            name = "criada_em",
            nullable = false,
            updatable = false
    )
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Embarcacao() {
    }

    public Embarcacao(
            Organizacao organizacao,
            Cliente proprietario,
            String nome,
            TipoEmbarcacao tipo,
            String fabricante,
            String modelo,
            Integer anoFabricacao,
            String numeroInscricao,
            String numeroCasco,
            String portoInscricao,
            String codigoPaisBandeira,
            BigDecimal comprimentoTotalMetros,
            BigDecimal bocaMetros,
            BigDecimal caladoMetros,
            BigDecimal pontalMetros,
            BigDecimal alturaTotalMetros,
            BigDecimal pesoKg,
            Integer capacidadePessoas,
            TipoPropulsao tipoPropulsao,
            String corPredominante,
            String observacoes
    ) {
        this.organizacao = organizacao;
        this.proprietario = proprietario;
        this.nome = nome;
        this.tipo = tipo;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.anoFabricacao = anoFabricacao;
        this.numeroInscricao = numeroInscricao;
        this.numeroCasco = numeroCasco;
        this.portoInscricao = portoInscricao;
        this.codigoPaisBandeira = codigoPaisBandeira;
        this.comprimentoTotalMetros = comprimentoTotalMetros;
        this.bocaMetros = bocaMetros;
        this.caladoMetros = caladoMetros;
        this.pontalMetros = pontalMetros;
        this.alturaTotalMetros = alturaTotalMetros;
        this.pesoKg = pesoKg;
        this.capacidadePessoas = capacidadePessoas;
        this.tipoPropulsao = tipoPropulsao;
        this.corPredominante = corPredominante;
        this.observacoes = observacoes;
        this.ativa = true;
        this.criadaEm = Instant.now();
        this.atualizadaEm = Instant.now();
    }

    public void atualizarDados(
            Cliente proprietario,
            String nome,
            TipoEmbarcacao tipo,
            String fabricante,
            String modelo,
            Integer anoFabricacao,
            String numeroInscricao,
            String numeroCasco,
            String portoInscricao,
            String codigoPaisBandeira,
            BigDecimal comprimentoTotalMetros,
            BigDecimal bocaMetros,
            BigDecimal caladoMetros,
            BigDecimal pontalMetros,
            BigDecimal alturaTotalMetros,
            BigDecimal pesoKg,
            Integer capacidadePessoas,
            TipoPropulsao tipoPropulsao,
            String corPredominante,
            String observacoes
    ) {
        this.proprietario = proprietario;
        this.nome = nome;
        this.tipo = tipo;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.anoFabricacao = anoFabricacao;
        this.numeroInscricao = numeroInscricao;
        this.numeroCasco = numeroCasco;
        this.portoInscricao = portoInscricao;
        this.codigoPaisBandeira = codigoPaisBandeira;
        this.comprimentoTotalMetros = comprimentoTotalMetros;
        this.bocaMetros = bocaMetros;
        this.caladoMetros = caladoMetros;
        this.pontalMetros = pontalMetros;
        this.alturaTotalMetros = alturaTotalMetros;
        this.pesoKg = pesoKg;
        this.capacidadePessoas = capacidadePessoas;
        this.tipoPropulsao = tipoPropulsao;
        this.corPredominante = corPredominante;
        this.observacoes = observacoes;
        this.atualizadaEm = Instant.now();
    }

    public void ativar() {
        this.ativa = true;
        this.atualizadaEm = Instant.now();
    }

    public void inativar() {
        this.ativa = false;
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Cliente getProprietario() {
        return proprietario;
    }

    public String getNome() {
        return nome;
    }

    public TipoEmbarcacao getTipo() {
        return tipo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getAnoFabricacao() {
        return anoFabricacao;
    }

    public String getNumeroInscricao() {
        return numeroInscricao;
    }

    public String getNumeroCasco() {
        return numeroCasco;
    }

    public String getPortoInscricao() {
        return portoInscricao;
    }

    public String getCodigoPaisBandeira() {
        return codigoPaisBandeira;
    }

    public BigDecimal getComprimentoTotalMetros() {
        return comprimentoTotalMetros;
    }

    public BigDecimal getBocaMetros() {
        return bocaMetros;
    }

    public BigDecimal getCaladoMetros() {
        return caladoMetros;
    }

    public BigDecimal getPontalMetros() {
        return pontalMetros;
    }

    public BigDecimal getAlturaTotalMetros() {
        return alturaTotalMetros;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public Integer getCapacidadePessoas() {
        return capacidadePessoas;
    }

    public TipoPropulsao getTipoPropulsao() {
        return tipoPropulsao;
    }

    public String getCorPredominante() {
        return corPredominante;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}