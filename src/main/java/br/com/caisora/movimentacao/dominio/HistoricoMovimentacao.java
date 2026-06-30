package br.com.caisora.movimentacao.dominio;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "historicos_movimentacoes")
public class HistoricoMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoMovimentacao tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusMovimentacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusMovimentacao statusNovo;

    @Column(name = "agendada_para_anterior")
    private Instant agendadaParaAnterior;

    @Column(name = "agendada_para_nova")
    private Instant agendadaParaNova;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 2000)
    private String observacao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_anteriores", columnDefinition = "jsonb")
    private Map<String, Object> dadosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_novos", columnDefinition = "jsonb")
    private Map<String, Object> dadosNovos;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    protected HistoricoMovimentacao() {
    }

    private HistoricoMovimentacao(
        Organizacao organizacao,
        Movimentacao movimentacao,
        TipoEventoMovimentacao tipoEvento,
        StatusMovimentacao statusAnterior,
        StatusMovimentacao statusNovo,
        Instant agendadaParaAnterior,
        Instant agendadaParaNova,
        Usuario usuario,
        String observacao,
        Map<String, Object> dadosAnteriores,
        Map<String, Object> dadosNovos,
        Instant ocorridoEm
    ) {
        this.organizacao = Objects.requireNonNull(organizacao, "Organizacao obrigatoria");
        this.movimentacao = Objects.requireNonNull(movimentacao, "Movimentacao obrigatoria");
        this.tipoEvento = Objects.requireNonNull(tipoEvento, "Tipo do evento obrigatorio");
        this.statusAnterior = statusAnterior;
        this.statusNovo = Objects.requireNonNull(statusNovo, "Novo status obrigatorio");
        this.agendadaParaAnterior = agendadaParaAnterior;
        this.agendadaParaNova = agendadaParaNova;
        this.usuario = Objects.requireNonNull(usuario, "Usuario obrigatorio");
        this.observacao = normalizarTexto(observacao);
        this.dadosAnteriores = copiarMapa(dadosAnteriores);
        this.dadosNovos = copiarMapa(dadosNovos);
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm, "Data do evento obrigatoria");
    }

    public static HistoricoMovimentacao criada(
        Movimentacao movimentacao,
        Usuario usuario,
        Instant ocorridoEm
    ) {
        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.CRIADA,
            null,
            StatusMovimentacao.AGENDADA,
            null,
            movimentacao.getAgendadaPara(),
            usuario,
            null,
            null,
            null,
            ocorridoEm
        );
    }

    public static HistoricoMovimentacao atualizada(
        Movimentacao movimentacao,
        Usuario usuario,
        Map<String, Object> dadosAnteriores,
        Map<String, Object> dadosNovos,
        String observacao,
        Instant ocorridoEm
    ) {
        exigirMapa(dadosAnteriores, "Dados anteriores obrigatorios");
        exigirMapa(dadosNovos, "Dados novos obrigatorios");

        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.ATUALIZADA,
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.AGENDADA,
            null,
            null,
            usuario,
            observacao,
            dadosAnteriores,
            dadosNovos,
            ocorridoEm
        );
    }

    public static HistoricoMovimentacao reagendada(
        Movimentacao movimentacao,
        Usuario usuario,
        Instant agendadaParaAnterior,
        Instant agendadaParaNova,
        String observacao,
        Instant ocorridoEm
    ) {
        Objects.requireNonNull(agendadaParaAnterior, "Agendamento anterior obrigatorio");
        Objects.requireNonNull(agendadaParaNova, "Novo agendamento obrigatorio");

        if (agendadaParaAnterior.equals(agendadaParaNova)) {
            throw new IllegalArgumentException(
                "O novo agendamento precisa ser diferente do anterior"
            );
        }

        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.REAGENDADA,
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.AGENDADA,
            agendadaParaAnterior,
            agendadaParaNova,
            usuario,
            observacao,
            null,
            null,
            ocorridoEm
        );
    }

    public static HistoricoMovimentacao iniciada(
        Movimentacao movimentacao,
        Usuario usuario,
        String observacao,
        Instant ocorridoEm
    ) {
        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.INICIADA,
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.EM_EXECUCAO,
            null,
            null,
            usuario,
            observacao,
            null,
            null,
            ocorridoEm
        );
    }

    public static HistoricoMovimentacao concluida(
        Movimentacao movimentacao,
        Usuario usuario,
        String observacao,
        Instant ocorridoEm
    ) {
        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.CONCLUIDA,
            StatusMovimentacao.EM_EXECUCAO,
            StatusMovimentacao.CONCLUIDA,
            null,
            null,
            usuario,
            observacao,
            null,
            null,
            ocorridoEm
        );
    }

    public static HistoricoMovimentacao cancelada(
        Movimentacao movimentacao,
        Usuario usuario,
        String motivo,
        Instant ocorridoEm
    ) {
        String observacao = normalizarTexto(motivo);
        if (observacao == null) {
            throw new IllegalArgumentException("Motivo do cancelamento obrigatorio");
        }

        return new HistoricoMovimentacao(
            movimentacao.getOrganizacao(),
            movimentacao,
            TipoEventoMovimentacao.CANCELADA,
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.CANCELADA,
            null,
            null,
            usuario,
            observacao,
            null,
            null,
            ocorridoEm
        );
    }

    private static void exigirMapa(Map<String, Object> mapa, String mensagem) {
        if (mapa == null) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static Map<String, Object> copiarMapa(Map<String, Object> mapa) {
        return mapa == null ? null : new LinkedHashMap<>(mapa);
    }

    private static String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    public UUID getId() { return id; }
    public Organizacao getOrganizacao() { return organizacao; }
    public Movimentacao getMovimentacao() { return movimentacao; }
    public TipoEventoMovimentacao getTipoEvento() { return tipoEvento; }
    public StatusMovimentacao getStatusAnterior() { return statusAnterior; }
    public StatusMovimentacao getStatusNovo() { return statusNovo; }
    public Instant getAgendadaParaAnterior() { return agendadaParaAnterior; }
    public Instant getAgendadaParaNova() { return agendadaParaNova; }
    public Usuario getUsuario() { return usuario; }
    public String getObservacao() { return observacao; }
    public Map<String, Object> getDadosAnteriores() { return dadosAnteriores; }
    public Map<String, Object> getDadosNovos() { return dadosNovos; }
    public Instant getOcorridoEm() { return ocorridoEm; }
}
