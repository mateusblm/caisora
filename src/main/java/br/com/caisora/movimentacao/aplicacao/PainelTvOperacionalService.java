package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.movimentacao.api.AcaoOperacionalPainelTv;
import br.com.caisora.movimentacao.api.LocalMovimentacaoPainelTvResponse;
import br.com.caisora.movimentacao.api.MovimentacaoPainelTvResponse;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.api.PainelTvOperacionalResponse;
import br.com.caisora.movimentacao.api.ResumoPainelTvResponse;
import br.com.caisora.movimentacao.api.SituacaoPainelTv;
import br.com.caisora.movimentacao.api.TipoAlertaPainelTv;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PainelTvOperacionalService {

    private static final ZoneId FUSO_OPERACIONAL =
        ZoneId.of("America/Sao_Paulo");

    private static final Duration JANELA_PROXIMA =
        Duration.ofMinutes(15);

    private static final int ATUALIZAR_APOS_SEGUNDOS = 15;

    private static final Collection<StatusMovimentacao>
        STATUS_ABERTOS = List.of(
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.EM_EXECUCAO
        );

    private final MovimentacaoRepository
        movimentacaoRepository;

    private final MovimentacaoMapper
        movimentacaoMapper;

    private final LeitorTokenJwt leitorTokenJwt;

    public PainelTvOperacionalService(
        MovimentacaoRepository
            movimentacaoRepository,
        MovimentacaoMapper movimentacaoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.movimentacaoRepository =
            movimentacaoRepository;
        this.movimentacaoMapper =
            movimentacaoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional(readOnly = true)
    public PainelTvOperacionalResponse buscar() {
        return buscar(Instant.now());
    }

    PainelTvOperacionalResponse buscar(
        Instant agora
    ) {
        UUID organizacaoId =
            leitorTokenJwt
                .obterUsuarioAutenticado()
                .organizacaoId();

        LocalDate dataLocal =
            agora
                .atZone(FUSO_OPERACIONAL)
                .toLocalDate();

        Instant inicioDia =
            dataLocal
                .atStartOfDay(FUSO_OPERACIONAL)
                .toInstant();

        Instant fimDiaExclusivo =
            dataLocal
                .plusDays(1)
                .atStartOfDay(FUSO_OPERACIONAL)
                .toInstant();

        Instant fimDiaInclusivo =
            fimDiaExclusivo.minusNanos(1);

        List<Movimentacao> abertas =
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusInAndAgendadaParaBeforeOrderByAgendadaParaAsc(
                    organizacaoId,
                    STATUS_ABERTOS,
                    fimDiaExclusivo
                );

        Comparator<Movimentacao>
            ordemAgendadas =
                criarOrdemAgendadas(agora);

        Comparator<Movimentacao>
            ordemEmExecucao =
                criarOrdemEmExecucao();

            List<Movimentacao> agendadas =
                abertas.stream()
                    .filter(
                        movimentacao ->
                            movimentacao.getStatus()
                                == StatusMovimentacao
                                    .AGENDADA
                    )
                    .toList();

            List<Movimentacao> emExecucao =
                abertas.stream()
                    .filter(
                        movimentacao ->
                            movimentacao.getStatus()
                                == StatusMovimentacao
                                    .EM_EXECUCAO
                    )
                    .sorted(ordemEmExecucao)
                    .toList();

            List<Movimentacao> descidas =
                filtrarPorTipo(
                    agendadas,
                    TipoMovimentacao.LANCAMENTO,
                    ordemAgendadas
                );

            List<Movimentacao> retiradas =
                filtrarPorTipo(
                    agendadas,
                    TipoMovimentacao.RETIRADA,
                    ordemAgendadas
                );

            List<Movimentacao> transferencias =
                filtrarPorTipo(
                    agendadas,
                    TipoMovimentacao.TRANSFERENCIA,
                    ordemAgendadas
                );

            List<Movimentacao>
                deslocamentosInternos =
                    filtrarPorTipo(
                        agendadas,
                        TipoMovimentacao
                            .DESLOCAMENTO_INTERNO,
                        ordemAgendadas
                    );

            List<Movimentacao> alertas =
                agendadas.stream()
                    .filter(
                        movimentacao ->
                            possuiAlerta(
                                movimentacao,
                                agora
                            )
                    )
                    .sorted(ordemAgendadas)
                    .toList();

            ResumoPainelTvResponse resumo =
                new ResumoPainelTvResponse(
                    descidas.size(),
                    retiradas.size(),
                    transferencias.size(),
                    deslocamentosInternos.size(),
                    emExecucao.size(),
                    alertas.size()
                );

            return new PainelTvOperacionalResponse(
                agora,
                FUSO_OPERACIONAL.getId(),
                inicioDia,
                fimDiaInclusivo,
                ATUALIZAR_APOS_SEGUNDOS,
                resumo,
                mapear(alertas, agora),
                mapear(descidas, agora),
                mapear(retiradas, agora),
                mapear(transferencias, agora),
                mapear(
                    deslocamentosInternos,
                    agora
                ),
                mapear(emExecucao, agora)
            );
    }

    private List<Movimentacao> filtrarPorTipo(
        List<Movimentacao> movimentacoes,
        TipoMovimentacao tipo,
        Comparator<Movimentacao> ordem
    ) {
        return movimentacoes.stream()
            .filter(
                movimentacao ->
                    movimentacao.getTipo() == tipo
            )
            .sorted(ordem)
            .toList();
    }

    private List<MovimentacaoPainelTvResponse>
        mapear(
            List<Movimentacao> movimentacoes,
            Instant agora
        ) {
        return movimentacoes.stream()
            .map(
                movimentacao ->
                    paraResponse(
                        movimentacao,
                        agora
                    )
            )
            .toList();
    }

    private MovimentacaoPainelTvResponse
        paraResponse(
            Movimentacao movimentacao,
            Instant agora
        ) {
        MovimentacaoResponse base =
            movimentacaoMapper.paraResponse(
                movimentacao
            );

        boolean emExecucao =
            base.status()
                == StatusMovimentacao
                    .EM_EXECUCAO;

        boolean atrasada =
            !emExecucao
            && base.agendadaPara()
                .isBefore(agora);

        boolean proxima =
            !emExecucao
            && !base.agendadaPara()
                .isBefore(agora)
            && !base.agendadaPara()
                .isAfter(
                    agora.plus(
                        JANELA_PROXIMA
                    )
                );

        long minutosAtraso =
            atrasada
                ? minutosArredondados(
                    Duration.between(
                        base.agendadaPara(),
                        agora
                    )
                )
                : 0;

        long minutosParaInicio =
            !emExecucao
            && !atrasada
                ? minutosArredondados(
                    Duration.between(
                        agora,
                        base.agendadaPara()
                    )
                )
                : 0;

        long minutosEmExecucao =
            emExecucao
            && base.iniciadaEm() != null
                ? minutosArredondados(
                    Duration.between(
                        base.iniciadaEm(),
                        agora
                    )
                )
                : 0;

        return new MovimentacaoPainelTvResponse(
            base.id(),
            base.embarcacaoId(),
            base.embarcacaoNome(),
            base.embarcacaoModelo(),
            base.proprietarioNome(),
            base.tipo(),
            obterAcaoOperacional(
                base.tipo()
            ),
            base.status(),
            base.prioridade(),
            obterSituacao(
                emExecucao,
                atrasada,
                proxima
            ),
            criarLocal(
                base.tipoPosicaoOrigem(),
                base.vagaOrigemCodigo(),
                base.descricaoOrigem()
            ),
            criarLocal(
                base.tipoPosicaoDestino(),
                base.vagaDestinoCodigo(),
                base.descricaoDestino()
            ),
            base.agendadaPara(),
            base.iniciadaEm(),
            base.operadorResponsavelNome(),
            base.observacoes(),
            minutosAtraso,
            minutosParaInicio,
            minutosEmExecucao,
            obterAlertas(
                base,
                atrasada,
                proxima
            )
        );
    }

    private LocalMovimentacaoPainelTvResponse
        criarLocal(
            TipoPosicaoEmbarcacao tipo,
            String vagaCodigo,
            String descricao
        ) {
        return new LocalMovimentacaoPainelTvResponse(
            tipo,
            vagaCodigo,
            descricao,
            obterRotuloLocal(
                tipo,
                vagaCodigo,
                descricao
            )
        );
    }

    private String obterRotuloLocal(
        TipoPosicaoEmbarcacao tipo,
        String vagaCodigo,
        String descricao
    ) {
        if (
            tipo == TipoPosicaoEmbarcacao.VAGA
        ) {
            return vagaCodigo == null
                ? "Vaga não informada"
                : "Vaga " + vagaCodigo;
        }

        String rotuloBase = switch (tipo) {
            case AGUA -> "Água";
            case PIER_ESPERA ->
                "Píer de espera";
            case AREA_SERVICO ->
                "Área de serviço";
            case EXTERNA ->
                "Área externa";
            case DESCONHECIDA ->
                "Posição desconhecida";
            case VAGA -> "Vaga";
        };

        if (
            descricao == null
            || descricao.isBlank()
        ) {
            return rotuloBase;
        }

        return rotuloBase
            + " · "
            + descricao.trim();
    }

    private AcaoOperacionalPainelTv
        obterAcaoOperacional(
            TipoMovimentacao tipo
        ) {
        return switch (tipo) {
            case LANCAMENTO ->
                AcaoOperacionalPainelTv
                    .DESCER_PARA_AGUA;
            case RETIRADA ->
                AcaoOperacionalPainelTv
                    .RETIRAR_DA_AGUA;
            case TRANSFERENCIA ->
                AcaoOperacionalPainelTv
                    .TRANSFERIR_DE_VAGA;
            case DESLOCAMENTO_INTERNO ->
                AcaoOperacionalPainelTv
                    .DESLOCAR_INTERNAMENTE;
        };
    }

    private SituacaoPainelTv obterSituacao(
        boolean emExecucao,
        boolean atrasada,
        boolean proxima
    ) {
        if (emExecucao) {
            return SituacaoPainelTv
                .EM_EXECUCAO;
        }

        if (atrasada) {
            return SituacaoPainelTv.ATRASADA;
        }

        if (proxima) {
            return SituacaoPainelTv.PROXIMA;
        }

        return SituacaoPainelTv.AGENDADA;
    }

    private List<TipoAlertaPainelTv>
        obterAlertas(
            MovimentacaoResponse movimentacao,
            boolean atrasada,
            boolean proxima
        ) {
        List<TipoAlertaPainelTv> alertas =
            new ArrayList<>();

        if (atrasada) {
            alertas.add(
                TipoAlertaPainelTv.ATRASADA
            );
        }

        if (
            movimentacao.prioridade()
                == PrioridadeMovimentacao
                    .URGENTE
        ) {
            alertas.add(
                TipoAlertaPainelTv.URGENTE
            );
        }

        if (proxima) {
            alertas.add(
                TipoAlertaPainelTv.PROXIMA
            );
        }

        if (
            movimentacao
                .operadorResponsavelId()
                == null
        ) {
            alertas.add(
                TipoAlertaPainelTv
                    .SEM_OPERADOR
            );
        }

        return alertas;
    }

    private boolean possuiAlerta(
        Movimentacao movimentacao,
        Instant agora
    ) {
        boolean atrasada =
            movimentacao
                .getAgendadaPara()
                .isBefore(agora);

        boolean urgente =
            movimentacao.getPrioridade()
                == PrioridadeMovimentacao
                    .URGENTE;

        boolean proxima =
            !atrasada
            && !movimentacao
                .getAgendadaPara()
                .isAfter(
                    agora.plus(
                        JANELA_PROXIMA
                    )
                );

        boolean semOperador =
            movimentacao
                .getOperadorResponsavel()
                == null;

        return atrasada
            || urgente
            || proxima
            || semOperador;
    }

    private Comparator<Movimentacao>
        criarOrdemAgendadas(
            Instant agora
        ) {
        return Comparator
            .comparingInt(
                (
                    Movimentacao movimentacao
                ) ->
                    pesoAtencao(
                        movimentacao,
                        agora
                    )
            )
            .thenComparingInt(
                PainelTvOperacionalService
                    ::pesoPrioridade
            )
            .thenComparing(
                Movimentacao::getAgendadaPara
            );
    }

    private Comparator<Movimentacao>
        criarOrdemEmExecucao() {
        return Comparator
            .<Movimentacao, Instant>comparing(
                Movimentacao::getIniciadaEm,
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            )
            .thenComparingInt(
                PainelTvOperacionalService
                    ::pesoPrioridade
            )
            .thenComparing(
                Movimentacao::getAgendadaPara
            );
    }

    private int pesoAtencao(
        Movimentacao movimentacao,
        Instant agora
    ) {
        if (
            movimentacao
                .getAgendadaPara()
                .isBefore(agora)
        ) {
            return 0;
        }

        if (
            movimentacao.getPrioridade()
                == PrioridadeMovimentacao
                    .URGENTE
        ) {
            return 1;
        }

        if (
            !movimentacao
                .getAgendadaPara()
                .isAfter(
                    agora.plus(
                        JANELA_PROXIMA
                    )
                )
        ) {
            return 2;
        }

        if (
            movimentacao
                .getOperadorResponsavel()
                == null
        ) {
            return 3;
        }

        return 4;
    }

    private static int pesoPrioridade(
        Movimentacao movimentacao
    ) {
        return switch (
            movimentacao.getPrioridade()
        ) {
            case URGENTE -> 0;
            case ALTA -> 1;
            case NORMAL -> 2;
        };
    }

    private static long minutosArredondados(
        Duration duracao
    ) {
        long segundos = Math.max(
            0,
            duracao.getSeconds()
        );

        if (segundos == 0) {
            return 0;
        }

        return (segundos + 59) / 60;
    }



}
