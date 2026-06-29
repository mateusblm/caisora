package br.com.caisora.vaga.dominio;

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
    name = "vagas",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vaga_organizacao_codigo",
            columnNames = {
                "organizacao_id",
                "codigo"
            }
        )
    }
)
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "organizacao_id",
        nullable = false
    )
    private Organizacao organizacao;

    @Column(
        nullable = false,
        length = 50
    )
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private TipoVaga tipo;

    @Column(length = 100)
    private String setor;

    @Column(length = 200)
    private String localizacao;

    @Column(
        name = "comprimento_maximo_metros",
        nullable = false,
        precision = 8,
        scale = 2
    )
    private BigDecimal comprimentoMaximoMetros;

    @Column(
        name = "boca_maxima_metros",
        nullable = false,
        precision = 8,
        scale = 2
    )
    private BigDecimal bocaMaximaMetros;

    @Column(
        name = "calado_maximo_metros",
        precision = 8,
        scale = 2
    )
    private BigDecimal caladoMaximoMetros;

    @Column(
        name = "altura_maxima_metros",
        precision = 8,
        scale = 2
    )
    private BigDecimal alturaMaximaMetros;

    @Column(
        name = "peso_maximo_kg",
        precision = 12,
        scale = 2
    )
    private BigDecimal pesoMaximoKg;

    @Column(
        name = "possui_agua",
        nullable = false
    )
    private boolean possuiAgua;

    @Column(
        name = "possui_energia",
        nullable = false
    )
    private boolean possuiEnergia;

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

    @Column(
        name = "atualizada_em",
        nullable = false
    )
    private Instant atualizadaEm;

    protected Vaga() {
    }

    public Vaga(
        Organizacao organizacao,
        String codigo,
        TipoVaga tipo,
        String setor,
        String localizacao,
        BigDecimal comprimentoMaximoMetros,
        BigDecimal bocaMaximaMetros,
        BigDecimal caladoMaximoMetros,
        BigDecimal alturaMaximaMetros,
        BigDecimal pesoMaximoKg,
        boolean possuiAgua,
        boolean possuiEnergia,
        String observacoes
    ) {
        this.organizacao = organizacao;
        this.codigo = codigo;
        this.tipo = tipo;
        this.setor = setor;
        this.localizacao = localizacao;
        this.comprimentoMaximoMetros =
            comprimentoMaximoMetros;
        this.bocaMaximaMetros =
            bocaMaximaMetros;
        this.caladoMaximoMetros =
            caladoMaximoMetros;
        this.alturaMaximaMetros =
            alturaMaximaMetros;
        this.pesoMaximoKg =
            pesoMaximoKg;
        this.possuiAgua =
            possuiAgua;
        this.possuiEnergia =
            possuiEnergia;
        this.observacoes =
            observacoes;
        this.ativa = true;
        this.criadaEm = Instant.now();
        this.atualizadaEm = Instant.now();
    }

    public void atualizarDados(
        String codigo,
        TipoVaga tipo,
        String setor,
        String localizacao,
        BigDecimal comprimentoMaximoMetros,
        BigDecimal bocaMaximaMetros,
        BigDecimal caladoMaximoMetros,
        BigDecimal alturaMaximaMetros,
        BigDecimal pesoMaximoKg,
        boolean possuiAgua,
        boolean possuiEnergia,
        String observacoes
    ) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.setor = setor;
        this.localizacao = localizacao;
        this.comprimentoMaximoMetros =
            comprimentoMaximoMetros;
        this.bocaMaximaMetros =
            bocaMaximaMetros;
        this.caladoMaximoMetros =
            caladoMaximoMetros;
        this.alturaMaximaMetros =
            alturaMaximaMetros;
        this.pesoMaximoKg =
            pesoMaximoKg;
        this.possuiAgua =
            possuiAgua;
        this.possuiEnergia =
            possuiEnergia;
        this.observacoes =
            observacoes;
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

    public String getCodigo() {
        return codigo;
    }

    public TipoVaga getTipo() {
        return tipo;
    }

    public String getSetor() {
        return setor;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public BigDecimal getComprimentoMaximoMetros() {
        return comprimentoMaximoMetros;
    }

    public BigDecimal getBocaMaximaMetros() {
        return bocaMaximaMetros;
    }

    public BigDecimal getCaladoMaximoMetros() {
        return caladoMaximoMetros;
    }

    public BigDecimal getAlturaMaximaMetros() {
        return alturaMaximaMetros;
    }

    public BigDecimal getPesoMaximoKg() {
        return pesoMaximoKg;
    }

    public boolean isPossuiAgua() {
        return possuiAgua;
    }

    public boolean isPossuiEnergia() {
        return possuiEnergia;
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