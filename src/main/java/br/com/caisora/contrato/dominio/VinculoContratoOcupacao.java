package br.com.caisora.contrato.dominio;

import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "vinculos_contrato_ocupacao")
public class VinculoContratoOcupacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocupacao_id", nullable = false)
    private Ocupacao ocupacao;

    @Column(name = "inicio_em", nullable = false)
    private Instant inicioEm;

    @Column(name = "fim_em")
    private Instant fimEm;

    @Column(name = "motivo_fim", length = 500)
    private String motivoFim;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected VinculoContratoOcupacao() {
    }

    public VinculoContratoOcupacao(
        Organizacao organizacao,
        Contrato contrato,
        Ocupacao ocupacao
    ) {
        this.organizacao = organizacao;
        this.contrato = contrato;
        this.ocupacao = ocupacao;
        this.inicioEm = Instant.now();
        this.criadoEm = Instant.now();
    }

    public void finalizar(String motivoFim) {
        if (!estaAberto()) {
            throw new IllegalStateException("Vinculo de ocupacao ja finalizado");
        }
        this.fimEm = Instant.now();
        this.motivoFim = motivoFim;
    }

    public boolean estaAberto() {
        return fimEm == null;
    }

    public UUID getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public Ocupacao getOcupacao() {
        return ocupacao;
    }

    public Instant getInicioEm() {
        return inicioEm;
    }

    public Instant getFimEm() {
        return fimEm;
    }

    public String getMotivoFim() {
        return motivoFim;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
