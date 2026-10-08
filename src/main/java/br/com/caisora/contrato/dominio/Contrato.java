package br.com.caisora.contrato.dominio;

import br.com.caisora.contrato.dominio.estado.EstadoContrato;
import br.com.caisora.contrato.dominio.estado.EstadosContrato;
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

    /** Inicia a montagem explícita de uma nova entidade (padrão Builder). */
    public static ContratoBuilder builder() {
        return new ContratoBuilder();
    }

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
        estadoAtual().editar();
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
        this.status = estadoAtual().enviarParaAssinatura();
        atualizarAuditoria();
    }

    public void ativar(LocalDate dataAssinatura) {
        StatusContrato proximoStatus = estadoAtual().ativar();
        if (dataAssinatura == null) {
            throw new IllegalArgumentException("Data de assinatura obrigatoria");
        }
        if (dataAssinatura.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de assinatura nao pode estar no futuro");
        }

        this.status = proximoStatus;
        this.dataAssinatura = dataAssinatura;
        this.dataAtivacao = LocalDate.now();
        atualizarAuditoria();
    }

    public void suspender() {
        this.status = estadoAtual().suspender();
        atualizarAuditoria();
    }

    public void reativar() {
        this.status = estadoAtual().reativar();
        atualizarAuditoria();
    }

    public void solicitarEncerramento() {
        this.status = estadoAtual().solicitarEncerramento();
        atualizarAuditoria();
    }

    public void encerrar(LocalDate dataEncerramento) {
        StatusContrato proximoStatus = estadoAtual().encerrar();
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

        this.status = proximoStatus;
        this.dataEncerramento = dataEncerramento;
        atualizarAuditoria();
    }

    public void cancelar() {
        this.status = estadoAtual().cancelar();
        atualizarAuditoria();
    }

    public boolean podeSerEditado() {
        return estadoAtual().podeSerEditado();
    }

    public boolean podeReceberOcupacao() {
        return estadoAtual().podeReceberOcupacao();
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

    private EstadoContrato estadoAtual() {
        // O enum guarda o dado no banco; o objeto State guarda o comportamento.
        return EstadosContrato.para(status);
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
