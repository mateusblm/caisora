package br.com.caisora.movimentacao.dominio;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.Usuario;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "embarcacao_id", nullable = false)
    private Embarcacao embarcacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimentacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMovimentacao status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeMovimentacao prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_posicao_origem", nullable = false, length = 30)
    private TipoPosicaoEmbarcacao tipoPosicaoOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_origem_id")
    private Vaga vagaOrigem;

    @Column(name = "descricao_origem", length = 255)
    private String descricaoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_posicao_destino", nullable = false, length = 30)
    private TipoPosicaoEmbarcacao tipoPosicaoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_destino_id")
    private Vaga vagaDestino;

    @Column(name = "descricao_destino", length = 255)
    private String descricaoDestino;

    @Column(name = "agendada_para", nullable = false)
    private Instant agendadaPara;

    @Column(name = "iniciada_em")
    private Instant iniciadaEm;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitada_por_id", nullable = false)
    private Usuario solicitadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_responsavel_id")
    private Usuario operadorResponsavel;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Movimentacao() {
    }

    public Movimentacao(
        Organizacao organizacao,
        Embarcacao embarcacao,
        TipoMovimentacao tipo,
        PrioridadeMovimentacao prioridade,
        TipoPosicaoEmbarcacao tipoPosicaoOrigem,
        Vaga vagaOrigem,
        String descricaoOrigem,
        TipoPosicaoEmbarcacao tipoPosicaoDestino,
        Vaga vagaDestino,
        String descricaoDestino,
        Instant agendadaPara,
        Usuario solicitadaPor,
        Usuario operadorResponsavel,
        String observacoes
    ) {
        validarDadosObrigatorios(
            organizacao,
            embarcacao,
            tipo,
            prioridade,
            tipoPosicaoOrigem,
            tipoPosicaoDestino,
            agendadaPara,
            solicitadaPor
        );

        validarPosicao(tipoPosicaoOrigem, vagaOrigem, "origem");
        validarPosicao(tipoPosicaoDestino, vagaDestino, "destino");
        validarRegrasDoTipo(
            tipo,
            tipoPosicaoOrigem,
            vagaOrigem,
            tipoPosicaoDestino,
            vagaDestino
        );

        Instant agora = Instant.now();

        this.organizacao = organizacao;
        this.embarcacao = embarcacao;
        this.tipo = tipo;
        this.status = StatusMovimentacao.AGENDADA;
        this.prioridade = prioridade;
        this.tipoPosicaoOrigem = tipoPosicaoOrigem;
        this.vagaOrigem = vagaOrigem;
        this.descricaoOrigem = normalizarTexto(descricaoOrigem);
        this.tipoPosicaoDestino = tipoPosicaoDestino;
        this.vagaDestino = vagaDestino;
        this.descricaoDestino = normalizarTexto(descricaoDestino);
        this.agendadaPara = agendadaPara;
        this.solicitadaPor = solicitadaPor;
        this.operadorResponsavel = operadorResponsavel;
        this.observacoes = normalizarTexto(observacoes);
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public void atualizarDados(
        PrioridadeMovimentacao prioridade,
        TipoPosicaoEmbarcacao tipoPosicaoDestino,
        Vaga vagaDestino,
        String descricaoDestino,
        Instant agendadaPara,
        Usuario operadorResponsavel,
        String observacoes
    ) {
        garantirAgendada();

        Objects.requireNonNull(prioridade, "Prioridade obrigatoria");
        Objects.requireNonNull(
            tipoPosicaoDestino,
            "Tipo da posicao de destino obrigatorio"
        );
        Objects.requireNonNull(agendadaPara, "Data de agendamento obrigatoria");

        validarPosicao(tipoPosicaoDestino, vagaDestino, "destino");
        validarRegrasDoTipo(
            this.tipo,
            this.tipoPosicaoOrigem,
            this.vagaOrigem,
            tipoPosicaoDestino,
            vagaDestino
        );

        this.prioridade = prioridade;
        this.tipoPosicaoDestino = tipoPosicaoDestino;
        this.vagaDestino = vagaDestino;
        this.descricaoDestino = normalizarTexto(descricaoDestino);
        this.agendadaPara = agendadaPara;
        this.operadorResponsavel = operadorResponsavel;
        this.observacoes = normalizarTexto(observacoes);
        this.atualizadaEm = Instant.now();
    }

    public void iniciar(Usuario operadorResponsavel, Instant iniciadaEm) {
        garantirAgendada();

        this.operadorResponsavel = Objects.requireNonNull(
            operadorResponsavel,
            "Operador responsavel obrigatorio"
        );
        this.iniciadaEm = Objects.requireNonNull(
            iniciadaEm,
            "Data de inicio obrigatoria"
        );
        this.status = StatusMovimentacao.EM_EXECUCAO;
        this.atualizadaEm = Instant.now();
    }

    public void concluir(Usuario operadorResponsavel, Instant concluidaEm) {
        garantirEmExecucao();

        Instant dataConclusao = Objects.requireNonNull(
            concluidaEm,
            "Data de conclusao obrigatoria"
        );

        if (dataConclusao.isBefore(this.iniciadaEm)) {
            throw new IllegalArgumentException(
                "A conclusao nao pode ser anterior ao inicio"
            );
        }

        if (operadorResponsavel != null) {
            this.operadorResponsavel = operadorResponsavel;
        }

        this.concluidaEm = dataConclusao;
        this.status = StatusMovimentacao.CONCLUIDA;
        this.atualizadaEm = Instant.now();
    }

    public void cancelar(Instant canceladaEm, String motivoCancelamento) {
        garantirAgendada();

        String motivo = normalizarTexto(motivoCancelamento);
        if (motivo == null) {
            throw new IllegalArgumentException("Motivo do cancelamento obrigatorio");
        }

        this.canceladaEm = Objects.requireNonNull(
            canceladaEm,
            "Data de cancelamento obrigatoria"
        );
        this.motivoCancelamento = motivo;
        this.status = StatusMovimentacao.CANCELADA;
        this.atualizadaEm = Instant.now();
    }

    public boolean estaAgendada() {
        return status == StatusMovimentacao.AGENDADA;
    }

    public boolean estaEmExecucao() {
        return status == StatusMovimentacao.EM_EXECUCAO;
    }

    public boolean estaConcluida() {
        return status == StatusMovimentacao.CONCLUIDA;
    }

    public boolean estaCancelada() {
        return status == StatusMovimentacao.CANCELADA;
    }

    public boolean estaAberta() {
        return estaAgendada() || estaEmExecucao();
    }

    private void garantirAgendada() {
        if (!estaAgendada()) {
            throw new IllegalStateException("A movimentacao precisa estar agendada");
        }
    }

    private void garantirEmExecucao() {
        if (!estaEmExecucao()) {
            throw new IllegalStateException("A movimentacao precisa estar em execucao");
        }
    }

    private static void validarDadosObrigatorios(
        Organizacao organizacao,
        Embarcacao embarcacao,
        TipoMovimentacao tipo,
        PrioridadeMovimentacao prioridade,
        TipoPosicaoEmbarcacao tipoPosicaoOrigem,
        TipoPosicaoEmbarcacao tipoPosicaoDestino,
        Instant agendadaPara,
        Usuario solicitadaPor
    ) {
        Objects.requireNonNull(organizacao, "Organizacao obrigatoria");
        Objects.requireNonNull(embarcacao, "Embarcacao obrigatoria");
        Objects.requireNonNull(tipo, "Tipo da movimentacao obrigatorio");
        Objects.requireNonNull(prioridade, "Prioridade obrigatoria");
        Objects.requireNonNull(
            tipoPosicaoOrigem,
            "Tipo da posicao de origem obrigatorio"
        );
        Objects.requireNonNull(
            tipoPosicaoDestino,
            "Tipo da posicao de destino obrigatorio"
        );
        Objects.requireNonNull(agendadaPara, "Data de agendamento obrigatoria");
        Objects.requireNonNull(solicitadaPor, "Usuario solicitante obrigatorio");
    }

    private static void validarPosicao(
        TipoPosicaoEmbarcacao tipoPosicao,
        Vaga vaga,
        String contexto
    ) {
        if (tipoPosicao == TipoPosicaoEmbarcacao.VAGA && vaga == null) {
            throw new IllegalArgumentException(
                "A vaga e obrigatoria para a " + contexto + " do tipo VAGA"
            );
        }

        if (tipoPosicao != TipoPosicaoEmbarcacao.VAGA && vaga != null) {
            throw new IllegalArgumentException(
                "A vaga deve ser nula para a "
                    + contexto
                    + " que nao seja do tipo VAGA"
            );
        }
    }

    private static void validarRegrasDoTipo(
        TipoMovimentacao tipo,
        TipoPosicaoEmbarcacao origem,
        Vaga vagaOrigem,
        TipoPosicaoEmbarcacao destino,
        Vaga vagaDestino
    ) {
        switch (tipo) {
            case LANCAMENTO -> {
                if (
                    origem != TipoPosicaoEmbarcacao.VAGA
                    || (
                        destino != TipoPosicaoEmbarcacao.AGUA
                        && destino != TipoPosicaoEmbarcacao.PIER_ESPERA
                    )
                ) {
                    throw new IllegalArgumentException(
                        "Lancamento deve sair de uma vaga e terminar na agua ou no pier de espera"
                    );
                }
            }
            case RETIRADA -> {
                boolean origemValida =
                    origem == TipoPosicaoEmbarcacao.AGUA
                    || origem == TipoPosicaoEmbarcacao.PIER_ESPERA
                    || origem == TipoPosicaoEmbarcacao.EXTERNA;

                if (!origemValida || destino != TipoPosicaoEmbarcacao.VAGA) {
                    throw new IllegalArgumentException(
                        "Retirada deve sair da agua, do pier ou de area externa e terminar em uma vaga"
                    );
                }
            }
            case TRANSFERENCIA -> {
                if (
                    origem != TipoPosicaoEmbarcacao.VAGA
                    || destino != TipoPosicaoEmbarcacao.VAGA
                    || vagaOrigem == null
                    || vagaDestino == null
                    || Objects.equals(vagaOrigem.getId(), vagaDestino.getId())
                ) {
                    throw new IllegalArgumentException(
                        "Transferencia exige vagas de origem e destino diferentes"
                    );
                }
            }
            case DESLOCAMENTO_INTERNO -> {
                if (
                    destino == TipoPosicaoEmbarcacao.VAGA
                    || destino == TipoPosicaoEmbarcacao.DESCONHECIDA
                ) {
                    throw new IllegalArgumentException(
                        "Deslocamento interno deve terminar no pier, area de servico, agua ou area externa"
                    );
                }
            }
        }
    }

    private static String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    public UUID getId() { return id; }
    public Organizacao getOrganizacao() { return organizacao; }
    public Embarcacao getEmbarcacao() { return embarcacao; }
    public TipoMovimentacao getTipo() { return tipo; }
    public StatusMovimentacao getStatus() { return status; }
    public PrioridadeMovimentacao getPrioridade() { return prioridade; }
    public TipoPosicaoEmbarcacao getTipoPosicaoOrigem() { return tipoPosicaoOrigem; }
    public Vaga getVagaOrigem() { return vagaOrigem; }
    public String getDescricaoOrigem() { return descricaoOrigem; }
    public TipoPosicaoEmbarcacao getTipoPosicaoDestino() { return tipoPosicaoDestino; }
    public Vaga getVagaDestino() { return vagaDestino; }
    public String getDescricaoDestino() { return descricaoDestino; }
    public Instant getAgendadaPara() { return agendadaPara; }
    public Instant getIniciadaEm() { return iniciadaEm; }
    public Instant getConcluidaEm() { return concluidaEm; }
    public Instant getCanceladaEm() { return canceladaEm; }
    public Usuario getSolicitadaPor() { return solicitadaPor; }
    public Usuario getOperadorResponsavel() { return operadorResponsavel; }
    public String getObservacoes() { return observacoes; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Long getVersao() { return versao; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
