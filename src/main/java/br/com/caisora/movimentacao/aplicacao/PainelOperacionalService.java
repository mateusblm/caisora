package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.movimentacao.api.IndicadoresOperacionaisResponse;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.api.PainelOperacionalResponse;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PainelOperacionalService {

    private static final ZoneId FUSO_OPERACIONAL =
        ZoneId.of("America/Sao_Paulo");

    private static final Duration JANELA_TRINTA_MINUTOS =
        Duration.ofMinutes(30);

    private static final Duration JANELA_DUAS_HORAS =
        Duration.ofHours(2);

    private static final Duration JANELA_UMA_HORA =
        Duration.ofHours(1);

    private static final int LIMITE_CONCLUIDAS_RECENTES = 5;

    private static final Collection<StatusMovimentacao>
        STATUS_ABERTOS = List.of(
            StatusMovimentacao.AGENDADA,
            StatusMovimentacao.EM_EXECUCAO
        );

    private static final Comparator<Movimentacao>
        ORDEM_AGENDADAS =
            Comparator
                .comparingInt(
                    PainelOperacionalService::pesoPrioridade
                )
                .thenComparing(
                    Movimentacao::getAgendadaPara
                )
                .thenComparing(
                    Movimentacao::getCriadaEm,
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                );

    private static final Comparator<Movimentacao>
        ORDEM_EM_EXECUCAO =
            Comparator
                .comparing(
                    Movimentacao::getIniciadaEm,
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                )
                .thenComparing(ORDEM_AGENDADAS);

    private final MovimentacaoRepository movimentacaoRepository;
    private final MovimentacaoMapper movimentacaoMapper;
    private final LeitorTokenJwt leitorTokenJwt;

    public PainelOperacionalService(
        MovimentacaoRepository movimentacaoRepository,
        MovimentacaoMapper movimentacaoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.movimentacaoMapper = movimentacaoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional(readOnly = true)
    public PainelOperacionalResponse buscar() {
        return buscar(Instant.now());
    }

    /*
     * Visibilidade de pacote para permitir testes determinísticos
     * sem depender do relógio real da máquina.
     */
    PainelOperacionalResponse buscar(Instant agora) {
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

        List<Movimentacao> concluidasHoje =
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusAndConcluidaEmBetweenOrderByConcluidaEmDesc(
                    organizacaoId,
                    StatusMovimentacao.CONCLUIDA,
                    inicioDia,
                    fimDiaInclusivo
                );

        Instant limiteTrintaMinutos =
            agora.plus(JANELA_TRINTA_MINUTOS);

        Instant limiteUmaHora =
            agora.plus(JANELA_UMA_HORA);

        Instant limiteDuasHoras =
            agora.plus(JANELA_DUAS_HORAS);

        List<Movimentacao> atrasadas =
            abertas.stream()
                .filter(Movimentacao::estaAgendada)
                .filter(
                    movimentacao ->
                        movimentacao
                            .getAgendadaPara()
                            .isBefore(agora)
                )
                .sorted(ORDEM_AGENDADAS)
                .toList();

        List<Movimentacao> emExecucao =
            abertas.stream()
                .filter(Movimentacao::estaEmExecucao)
                .sorted(ORDEM_EM_EXECUCAO)
                .toList();

        List<Movimentacao> proximosTrintaMinutos =
            abertas.stream()
                .filter(Movimentacao::estaAgendada)
                .filter(
                    movimentacao ->
                        !movimentacao
                            .getAgendadaPara()
                            .isBefore(agora)
                )
                .filter(
                    movimentacao ->
                        !movimentacao
                            .getAgendadaPara()
                            .isAfter(limiteTrintaMinutos)
                )
                .sorted(ORDEM_AGENDADAS)
                .toList();

        List<Movimentacao> proximasDuasHoras =
            abertas.stream()
                .filter(Movimentacao::estaAgendada)
                .filter(
                    movimentacao ->
                        movimentacao
                            .getAgendadaPara()
                            .isAfter(limiteTrintaMinutos)
                )
                .filter(
                    movimentacao ->
                        !movimentacao
                            .getAgendadaPara()
                            .isAfter(limiteDuasHoras)
                )
                .sorted(ORDEM_AGENDADAS)
                .toList();

        List<Movimentacao> restanteDia =
            abertas.stream()
                .filter(Movimentacao::estaAgendada)
                .filter(
                    movimentacao ->
                        movimentacao
                            .getAgendadaPara()
                            .isAfter(limiteDuasHoras)
                )
                .sorted(ORDEM_AGENDADAS)
                .toList();

        long proximaHora =
            abertas.stream()
                .filter(Movimentacao::estaAgendada)
                .filter(
                    movimentacao ->
                        !movimentacao
                            .getAgendadaPara()
                            .isBefore(agora)
                )
                .filter(
                    movimentacao ->
                        !movimentacao
                            .getAgendadaPara()
                            .isAfter(limiteUmaHora)
                )
                .count();

        long urgentes =
            abertas.stream()
                .filter(
                    movimentacao ->
                        movimentacao.getPrioridade()
                            == PrioridadeMovimentacao.URGENTE
                )
                .count();

        long semOperador =
            abertas.stream()
                .filter(
                    movimentacao ->
                        movimentacao
                            .getOperadorResponsavel()
                            == null
                )
                .count();

        IndicadoresOperacionaisResponse indicadores =
            new IndicadoresOperacionaisResponse(
                emExecucao.size(),
                atrasadas.size(),
                proximaHora,
                urgentes,
                semOperador,
                concluidasHoje.size()
            );

        List<MovimentacaoResponse>
            concluidasRecentemente =
                concluidasHoje.stream()
                    .limit(
                        LIMITE_CONCLUIDAS_RECENTES
                    )
                    .map(
                        movimentacaoMapper::paraResponse
                    )
                    .toList();

        return new PainelOperacionalResponse(
            agora,
            FUSO_OPERACIONAL.getId(),
            inicioDia,
            fimDiaInclusivo,
            indicadores,
            mapear(atrasadas),
            mapear(emExecucao),
            mapear(proximosTrintaMinutos),
            mapear(proximasDuasHoras),
            mapear(restanteDia),
            concluidasRecentemente
        );
    }

    private List<MovimentacaoResponse> mapear(
        List<Movimentacao> movimentacoes
    ) {
        return movimentacoes.stream()
            .map(movimentacaoMapper::paraResponse)
            .toList();
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
}
