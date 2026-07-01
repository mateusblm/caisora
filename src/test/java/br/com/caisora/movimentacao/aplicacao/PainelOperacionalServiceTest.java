package br.com.caisora.movimentacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.api.PainelOperacionalResponse;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PainelOperacionalServiceTest {

    private static final UUID ORGANIZACAO_ID =
        UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
        );

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @Mock
    private MovimentacaoMapper movimentacaoMapper;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private PainelOperacionalService service;

    @BeforeEach
    void configurar() {
        service =
            new PainelOperacionalService(
                movimentacaoRepository,
                movimentacaoMapper,
                leitorTokenJwt
            );

        when(
            leitorTokenJwt.obterUsuarioAutenticado()
        ).thenReturn(
            new UsuarioAutenticado(
                UUID.fromString(
                    "20000000-0000-0000-0000-000000000001"
                ),
                "Operador",
                "operador@caisora.com",
                PerfilUsuario.ADMINISTRADOR_MARINA,
                ORGANIZACAO_ID,
                "Marina Caisora"
            )
        );

        when(
            movimentacaoMapper.paraResponse(
                any(Movimentacao.class)
            )
        ).thenAnswer(
            invocation ->
                resposta(
                    invocation.getArgument(0)
                )
        );
    }

    @Test
    void deveClassificarMovimentacoesDoDia() {
        Instant agora =
            Instant.parse(
                "2026-06-30T15:00:00Z"
            );

        Movimentacao atrasada =
            movimentacao(
                "30000000-0000-0000-0000-000000000001",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.URGENTE,
                agora.minusSeconds(30 * 60),
                null,
                false
            );

        Movimentacao emExecucao =
            movimentacao(
                "30000000-0000-0000-0000-000000000002",
                StatusMovimentacao.EM_EXECUCAO,
                PrioridadeMovimentacao.NORMAL,
                agora.minusSeconds(60 * 60),
                agora.minusSeconds(20 * 60),
                true
            );

        Movimentacao proxima =
            movimentacao(
                "30000000-0000-0000-0000-000000000003",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.ALTA,
                agora.plusSeconds(20 * 60),
                null,
                true
            );

        Movimentacao duasHoras =
            movimentacao(
                "30000000-0000-0000-0000-000000000004",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.NORMAL,
                agora.plusSeconds(90 * 60),
                null,
                false
            );

        Movimentacao restanteDia =
            movimentacao(
                "30000000-0000-0000-0000-000000000005",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.URGENTE,
                agora.plusSeconds(4 * 60 * 60),
                null,
                false
            );

        Movimentacao concluida =
            movimentacao(
                "30000000-0000-0000-0000-000000000006",
                StatusMovimentacao.CONCLUIDA,
                PrioridadeMovimentacao.NORMAL,
                agora.minusSeconds(2 * 60 * 60),
                agora.minusSeconds(100 * 60),
                true
            );

        when(
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusInAndAgendadaParaBeforeOrderByAgendadaParaAsc(
                    any(UUID.class),
                    any(Collection.class),
                    any(Instant.class)
                )
        ).thenReturn(
            List.of(
                atrasada,
                emExecucao,
                proxima,
                duasHoras,
                restanteDia
            )
        );

        when(
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusAndConcluidaEmBetweenOrderByConcluidaEmDesc(
                    any(UUID.class),
                    any(StatusMovimentacao.class),
                    any(Instant.class),
                    any(Instant.class)
                )
        ).thenReturn(List.of(concluida));

        PainelOperacionalResponse painel =
            service.buscar(agora);

        assertThat(painel.atrasadas())
            .hasSize(1);

        assertThat(painel.emExecucao())
            .hasSize(1);

        assertThat(
            painel.proximosTrintaMinutos()
        ).hasSize(1);

        assertThat(painel.proximasDuasHoras())
            .hasSize(1);

        assertThat(painel.restanteDia())
            .hasSize(1);

        assertThat(
            painel.concluidasRecentemente()
        ).hasSize(1);

        assertThat(
            painel.indicadores().emExecucao()
        ).isEqualTo(1);

        assertThat(
            painel.indicadores().atrasadas()
        ).isEqualTo(1);

        assertThat(
            painel.indicadores().proximaHora()
        ).isEqualTo(1);

        assertThat(
            painel.indicadores().urgentes()
        ).isEqualTo(2);

        assertThat(
            painel.indicadores().semOperador()
        ).isEqualTo(3);

        assertThat(
            painel.indicadores().concluidasHoje()
        ).isEqualTo(1);

        assertThat(painel.fusoHorario())
            .isEqualTo("America/Sao_Paulo");

        verify(
            movimentacaoRepository
        )
            .findAllByOrganizacaoIdAndStatusInAndAgendadaParaBeforeOrderByAgendadaParaAsc(
                any(UUID.class),
                any(Collection.class),
                any(Instant.class)
            );
    }

    @Test
    void deveOrdenarAgendadasPorPrioridadeEHorario() {
        Instant agora =
            Instant.parse(
                "2026-06-30T15:00:00Z"
            );

        Movimentacao normal =
            movimentacao(
                "40000000-0000-0000-0000-000000000001",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.NORMAL,
                agora.plusSeconds(10 * 60),
                null,
                false
            );

        Movimentacao urgente =
            movimentacao(
                "40000000-0000-0000-0000-000000000002",
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.URGENTE,
                agora.plusSeconds(20 * 60),
                null,
                false
            );

        when(
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusInAndAgendadaParaBeforeOrderByAgendadaParaAsc(
                    any(UUID.class),
                    any(Collection.class),
                    any(Instant.class)
                )
        ).thenReturn(
            List.of(normal, urgente)
        );

        when(
            movimentacaoRepository
                .findAllByOrganizacaoIdAndStatusAndConcluidaEmBetweenOrderByConcluidaEmDesc(
                    any(UUID.class),
                    any(StatusMovimentacao.class),
                    any(Instant.class),
                    any(Instant.class)
                )
        ).thenReturn(List.of());

        PainelOperacionalResponse painel =
            service.buscar(agora);

        assertThat(
            painel.proximosTrintaMinutos()
        )
            .extracting(MovimentacaoResponse::id)
            .containsExactly(
                urgente.getId(),
                normal.getId()
            );
    }

    private Movimentacao movimentacao(
        String id,
        StatusMovimentacao status,
        PrioridadeMovimentacao prioridade,
        Instant agendadaPara,
        Instant iniciadaEm,
        boolean possuiOperador
    ) {
        Movimentacao movimentacao =
            mock(Movimentacao.class);

        when(movimentacao.getId())
            .thenReturn(UUID.fromString(id));

        when(movimentacao.getTipo())
            .thenReturn(
                TipoMovimentacao.LANCAMENTO
            );

        when(movimentacao.getStatus())
            .thenReturn(status);

        when(movimentacao.getPrioridade())
            .thenReturn(prioridade);

        when(movimentacao.getAgendadaPara())
            .thenReturn(agendadaPara);

        when(movimentacao.getIniciadaEm())
            .thenReturn(iniciadaEm);

        when(movimentacao.getCriadaEm())
            .thenReturn(
                agendadaPara.minusSeconds(60 * 60)
            );

if (
    status == StatusMovimentacao.AGENDADA
    || status == StatusMovimentacao.EM_EXECUCAO
    ) {
        when(movimentacao.estaAgendada())
            .thenReturn(
                status == StatusMovimentacao.AGENDADA
            );

        when(movimentacao.estaEmExecucao())
            .thenReturn(
                status
                    == StatusMovimentacao.EM_EXECUCAO
            );

        if (possuiOperador) {
            when(
                movimentacao
                    .getOperadorResponsavel()
            ).thenReturn(mock(Usuario.class));
        }
    }
        return movimentacao;
    }

    private MovimentacaoResponse resposta(
        Movimentacao movimentacao
    ) {
        return new MovimentacaoResponse(
            movimentacao.getId(),
            UUID.fromString(
                "50000000-0000-0000-0000-000000000001"
            ),
            "Aurora",
            "V33",
            "João",
            movimentacao.getTipo(),
            movimentacao.getStatus(),
            movimentacao.getPrioridade(),
            TipoPosicaoEmbarcacao.VAGA,
            UUID.fromString(
                "60000000-0000-0000-0000-000000000001"
            ),
            "A-01",
            null,
            TipoPosicaoEmbarcacao.AGUA,
            null,
            null,
            "Rampa principal",
            movimentacao.getAgendadaPara(),
            movimentacao.getIniciadaEm(),
            null,
            null,
            UUID.fromString(
                "70000000-0000-0000-0000-000000000001"
            ),
            "Administrador",
            null,
            null,
            null,
            null,
            0L,
            ORGANIZACAO_ID,
            movimentacao.getCriadaEm(),
            movimentacao.getCriadaEm()
        );
    }
}
