package br.com.caisora.contrato.dominio;

import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
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
@Table(name = "historicos_contrato")
public class HistoricoContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEventoContrato tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusContrato statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 30)
    private StatusContrato statusNovo;

    @Column(length = 1000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private Usuario realizadoPor;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private Instant realizadoEm;

    protected HistoricoContrato() {
    }

    public HistoricoContrato(
        Organizacao organizacao,
        Contrato contrato,
        TipoEventoContrato tipoEvento,
        StatusContrato statusAnterior,
        StatusContrato statusNovo,
        String descricao,
        Usuario realizadoPor
    ) {
        this.organizacao = organizacao;
        this.contrato = contrato;
        this.tipoEvento = tipoEvento;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.descricao = descricao;
        this.realizadoPor = realizadoPor;
        this.realizadoEm = Instant.now();
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

    public TipoEventoContrato getTipoEvento() {
        return tipoEvento;
    }

    public StatusContrato getStatusAnterior() {
        return statusAnterior;
    }

    public StatusContrato getStatusNovo() {
        return statusNovo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Usuario getRealizadoPor() {
        return realizadoPor;
    }

    public Instant getRealizadoEm() {
        return realizadoEm;
    }
}
