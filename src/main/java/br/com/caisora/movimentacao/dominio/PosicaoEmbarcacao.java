package br.com.caisora.movimentacao.dominio;

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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "posicoes_embarcacoes")
public class PosicaoEmbarcacao {

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
    private TipoPosicaoEmbarcacao tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id")
    private Vaga vaga;

    @Column(name = "descricao_local", length = 255)
    private String descricaoLocal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimentacao_origem_id")
    private Movimentacao movimentacaoOrigem;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected PosicaoEmbarcacao() {
    }

    public PosicaoEmbarcacao(
        Organizacao organizacao,
        Embarcacao embarcacao,
        TipoPosicaoEmbarcacao tipo,
        Vaga vaga,
        String descricaoLocal
    ) {
        validarPosicao(tipo, vaga);

        Instant agora = Instant.now();

        this.organizacao = Objects.requireNonNull(
            organizacao,
            "Organizacao obrigatoria"
        );
        this.embarcacao = Objects.requireNonNull(
            embarcacao,
            "Embarcacao obrigatoria"
        );
        this.tipo = tipo;
        this.vaga = vaga;
        this.descricaoLocal = normalizarTexto(descricaoLocal);
        this.movimentacaoOrigem = null;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public static PosicaoEmbarcacao criarEmVaga(
        Organizacao organizacao,
        Embarcacao embarcacao,
        Vaga vaga
    ) {
        return new PosicaoEmbarcacao(
            organizacao,
            embarcacao,
            TipoPosicaoEmbarcacao.VAGA,
            vaga,
            null
        );
    }

    public static PosicaoEmbarcacao criarDesconhecida(
        Organizacao organizacao,
        Embarcacao embarcacao
    ) {
        return new PosicaoEmbarcacao(
            organizacao,
            embarcacao,
            TipoPosicaoEmbarcacao.DESCONHECIDA,
            null,
            null
        );
    }

    public void atualizar(
        TipoPosicaoEmbarcacao tipo,
        Vaga vaga,
        String descricaoLocal,
        Movimentacao movimentacaoOrigem
    ) {
        validarPosicao(tipo, vaga);
        this.tipo = tipo;
        this.vaga = vaga;
        this.descricaoLocal = normalizarTexto(descricaoLocal);
        this.movimentacaoOrigem = movimentacaoOrigem;
        this.atualizadaEm = Instant.now();
    }

    public boolean estaEmVaga() {
        return tipo == TipoPosicaoEmbarcacao.VAGA;
    }

    private static void validarPosicao(TipoPosicaoEmbarcacao tipo, Vaga vaga) {
        Objects.requireNonNull(tipo, "Tipo da posicao obrigatorio");

        if (tipo == TipoPosicaoEmbarcacao.VAGA && vaga == null) {
            throw new IllegalArgumentException(
                "A vaga e obrigatoria quando a posicao for VAGA"
            );
        }

        if (tipo != TipoPosicaoEmbarcacao.VAGA && vaga != null) {
            throw new IllegalArgumentException(
                "A vaga deve ser nula quando a posicao nao for VAGA"
            );
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
    public TipoPosicaoEmbarcacao getTipo() { return tipo; }
    public Vaga getVaga() { return vaga; }
    public String getDescricaoLocal() { return descricaoLocal; }
    public Movimentacao getMovimentacaoOrigem() { return movimentacaoOrigem; }
    public Long getVersao() { return versao; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
