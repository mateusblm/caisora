package br.com.caisora.contrato.dominio;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.TipoVaga;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "contratos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_contrato_organizacao_numero",
            columnNames = {"organizacao_id", "numero"}
        )
    }
)
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "embarcacao_id", nullable = false)
    private Embarcacao embarcacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusContrato status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vaga_contratada", nullable = false, length = 30)
    private TipoVaga tipoVagaContratada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodicidadeContrato periodicidade;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_assinatura")
    private LocalDate dataAssinatura;

    @Column(name = "data_ativacao")
    private LocalDate dataAtivacao;

    @Column(name = "data_encerramento")
    private LocalDate dataEncerramento;

    @Column(name = "renovacao_automatica", nullable = false)
    private boolean renovacaoAutomatica;

    @Column(name = "dias_aviso_previo")
    private Integer diasAvisoPrevio;

    @Column(name = "valor_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBase;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(length = 2000)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    protected Contrato() {
    }

    public Contrato(
        Organizacao organizacao,
        String numero,
        Cliente cliente,
        Embarcacao embarcacao,
        TipoVaga tipoVagaContratada,
        PeriodicidadeContrato periodicidade,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean renovacaoAutomatica,
        Integer diasAvisoPrevio,
        BigDecimal valorBase,
        Integer diaVencimento,
        String observacoes,
        Usuario criadoPor
    ) {
        validarDadosComerciais(
            periodicidade,
            dataInicio,
            dataFim,
            diasAvisoPrevio,
            valorBase,
            diaVencimento
        );

        this.organizacao = Objects.requireNonNull(
            organizacao,
            "Organizacao obrigatoria"
        );
        this.numero = Objects.requireNonNull(
            numero,
            "Numero do contrato obrigatorio"
        );
        this.cliente = Objects.requireNonNull(
            cliente,
            "Cliente obrigatorio"
        );
        this.embarcacao = Objects.requireNonNull(
            embarcacao,
            "Embarcacao obrigatoria"
        );
        this.status = StatusContrato.RASCUNHO;
        this.tipoVagaContratada = Objects.requireNonNull(
            tipoVagaContratada,
            "Tipo de vaga contratada obrigatorio"
        );
        this.periodicidade = periodicidade;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.renovacaoAutomatica = renovacaoAutomatica;
        this.diasAvisoPrevio = diasAvisoPrevio;
        this.valorBase = valorBase;
        this.diaVencimento = diaVencimento;
        this.observacoes = observacoes;
        this.criadoPor = Objects.requireNonNull(
            criadoPor,
            "Usuario criador obrigatorio"
        );
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public void atualizarDados(
        Cliente cliente,
        Embarcacao embarcacao,
        TipoVaga tipoVagaContratada,
        PeriodicidadeContrato periodicidade,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean renovacaoAutomatica,
        Integer diasAvisoPrevio,
        BigDecimal valorBase,
        Integer diaVencimento,
        String observacoes
    ) {
        exigirStatus(StatusContrato.RASCUNHO);
        validarDadosComerciais(
            periodicidade,
            dataInicio,
            dataFim,
            diasAvisoPrevio,
            valorBase,
            diaVencimento
        );

        this.cliente = Objects.requireNonNull(
            cliente,
            "Cliente obrigatorio"
        );
        this.embarcacao = Objects.requireNonNull(
            embarcacao,
            "Embarcacao obrigatoria"
        );
        this.tipoVagaContratada = Objects.requireNonNull(
            tipoVagaContratada,
            "Tipo de vaga contratada obrigatorio"
        );
        this.periodicidade = periodicidade;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.renovacaoAutomatica = renovacaoAutomatica;
        this.diasAvisoPrevio = diasAvisoPrevio;
        this.valorBase = valorBase;
        this.diaVencimento = diaVencimento;
        this.observacoes = observacoes;
        atualizarAuditoria();
    }

    public void enviarParaAssinatura() {
        exigirStatus(StatusContrato.RASCUNHO);
        this.status = StatusContrato.PENDENTE_ASSINATURA;
        atualizarAuditoria();
    }

    public void ativar(LocalDate dataAssinatura) {
        exigirStatus(StatusContrato.PENDENTE_ASSINATURA);
        if (dataAssinatura == null) {
            throw new IllegalArgumentException("Data de assinatura obrigatoria");
        }
        if (dataAssinatura.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de assinatura nao pode estar no futuro");
        }

        this.status = StatusContrato.ATIVO;
        this.dataAssinatura = dataAssinatura;
        this.dataAtivacao = LocalDate.now();
        atualizarAuditoria();
    }

    public void suspender() {
        exigirStatus(StatusContrato.ATIVO);
        this.status = StatusContrato.SUSPENSO;
        atualizarAuditoria();
    }

    public void reativar() {
        exigirStatus(StatusContrato.SUSPENSO);
        this.status = StatusContrato.ATIVO;
        atualizarAuditoria();
    }

    public void solicitarEncerramento() {
        if (
            status != StatusContrato.ATIVO
                && status != StatusContrato.SUSPENSO
        ) {
            throw new IllegalStateException(
                "Somente contrato ativo ou suspenso pode entrar em encerramento"
            );
        }

        this.status = StatusContrato.EM_ENCERRAMENTO;
        atualizarAuditoria();
    }

    public void encerrar(LocalDate dataEncerramento) {
        exigirStatus(StatusContrato.EM_ENCERRAMENTO);
        if (dataEncerramento == null) {
            throw new IllegalArgumentException("Data de encerramento obrigatoria");
        }
        if (dataEncerramento.isBefore(dataInicio)) {
            throw new IllegalArgumentException(
                "Data de encerramento nao pode ser anterior ao inicio"
            );
        }
        if (
            dataAtivacao != null
                && dataEncerramento.isBefore(dataAtivacao)
        ) {
            throw new IllegalArgumentException(
                "Data de encerramento nao pode ser anterior a ativacao"
            );
        }
        if (dataEncerramento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Data de encerramento nao pode estar no futuro"
            );
        }

        this.status = StatusContrato.ENCERRADO;
        this.dataEncerramento = dataEncerramento;
        atualizarAuditoria();
    }

    public void cancelar() {
        if (
            status != StatusContrato.RASCUNHO
                && status != StatusContrato.PENDENTE_ASSINATURA
        ) {
            throw new IllegalStateException(
                "Somente contrato em rascunho ou pendente de assinatura pode ser cancelado"
            );
        }

        this.status = StatusContrato.CANCELADO;
        atualizarAuditoria();
    }

    public boolean podeSerEditado() {
        return status == StatusContrato.RASCUNHO;
    }

    public boolean podeReceberOcupacao() {
        return status == StatusContrato.ATIVO;
    }

    private void validarDadosComerciais(
        PeriodicidadeContrato periodicidade,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer diasAvisoPrevio,
        BigDecimal valorBase,
        Integer diaVencimento
    ) {
        if (periodicidade == null) {
            throw new IllegalArgumentException("Periodicidade obrigatoria");
        }
        if (dataInicio == null) {
            throw new IllegalArgumentException("Data de inicio obrigatoria");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException(
                "Data de fim nao pode ser anterior a data de inicio"
            );
        }
        if (
            periodicidade == PeriodicidadeContrato.PERSONALIZADA
                && dataFim == null
        ) {
            throw new IllegalArgumentException(
                "Data de fim obrigatoria para periodicidade personalizada"
            );
        }
        if (diasAvisoPrevio != null && diasAvisoPrevio < 0) {
            throw new IllegalArgumentException(
                "Dias de aviso previo nao pode ser negativo"
            );
        }
        if (valorBase == null || valorBase.signum() < 0) {
            throw new IllegalArgumentException(
                "Valor base deve ser maior ou igual a zero"
            );
        }
        if (
            diaVencimento == null
                || diaVencimento < 1
                || diaVencimento > 31
        ) {
            throw new IllegalArgumentException(
                "Dia de vencimento deve estar entre 1 e 31"
            );
        }
    }

    private void exigirStatus(StatusContrato statusEsperado) {
        if (status != statusEsperado) {
            throw new IllegalStateException(
                "Operacao invalida para contrato no status " + status
            );
        }
    }

    private void atualizarAuditoria() {
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public String getNumero() {
        return numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Embarcacao getEmbarcacao() {
        return embarcacao;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public TipoVaga getTipoVagaContratada() {
        return tipoVagaContratada;
    }

    public PeriodicidadeContrato getPeriodicidade() {
        return periodicidade;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public LocalDate getDataAssinatura() {
        return dataAssinatura;
    }

    public LocalDate getDataAtivacao() {
        return dataAtivacao;
    }

    public LocalDate getDataEncerramento() {
        return dataEncerramento;
    }

    public boolean isRenovacaoAutomatica() {
        return renovacaoAutomatica;
    }

    public Integer getDiasAvisoPrevio() {
        return diasAvisoPrevio;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public Usuario getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
