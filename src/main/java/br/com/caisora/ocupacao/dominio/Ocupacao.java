package br.com.caisora.ocupacao.dominio;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.vaga.dominio.Vaga;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ocupacoes")
public class Ocupacao {

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

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "embarcacao_id",
        nullable = false
    )
    private Embarcacao embarcacao;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "vaga_id",
        nullable = false
    )
    private Vaga vaga;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private StatusOcupacao status;

    @Column(
        name = "inicio_em",
        nullable = false
    )
    private Instant inicioEm;

    @Column(name = "fim_previsto_em")
    private Instant fimPrevistoEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    @Column(length = 2000)
    private String observacoes;

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

    protected Ocupacao() {
    }

    public Ocupacao(
        Organizacao organizacao,
        Embarcacao embarcacao,
        Vaga vaga,
        Instant inicioEm,
        Instant fimPrevistoEm,
        String observacoes
    ) {
        this.organizacao = organizacao;
        this.embarcacao = embarcacao;
        this.vaga = vaga;
        this.status = StatusOcupacao.ATIVA;
        this.inicioEm = inicioEm;
        this.fimPrevistoEm = fimPrevistoEm;
        this.encerradaEm = null;
        this.observacoes = observacoes;
        this.criadaEm = Instant.now();
        this.atualizadaEm = Instant.now();
    }

    public void atualizarDados(
        Instant fimPrevistoEm,
        String observacoes
    ) {
        this.fimPrevistoEm = fimPrevistoEm;
        this.observacoes = observacoes;
        this.atualizadaEm = Instant.now();
    }

    public void encerrar(
        Instant encerradaEm
    ) {
        this.status =
            StatusOcupacao.ENCERRADA;

        this.encerradaEm =
            encerradaEm;

        this.atualizadaEm =
            Instant.now();
    }

    public boolean estaAtiva() {
        return status == StatusOcupacao.ATIVA;
    }

    public UUID getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Embarcacao getEmbarcacao() {
        return embarcacao;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public StatusOcupacao getStatus() {
        return status;
    }

    public Instant getInicioEm() {
        return inicioEm;
    }

    public Instant getFimPrevistoEm() {
        return fimPrevistoEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}