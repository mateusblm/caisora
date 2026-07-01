package br.com.caisora.movimentacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.movimentacao.api.AcaoOperacionalPainelTv;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.api.PainelTvOperacionalResponse;
import br.com.caisora.movimentacao.api.SituacaoPainelTv;
import br.com.caisora.movimentacao.api.TipoAlertaPainelTv;
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
class PainelTvOperacionalServiceTest {

    private static final UUID ORGANIZACAO_ID =
        UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
        );

    @Mock
    private MovimentacaoRepository
        movimentacaoRepository;

    @Mock
    private MovimentacaoMapper movimentacaoMapper;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private PainelTvOperacionalService service;

    @BeforeEach
    void configurar() {
        service =
            new PainelTvOperacionalService(
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
                PerfilUsuario
                    .ADMINISTRADOR_MARINA,
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
                respostaBase(
                    invocation.getArgument(0)
                )
        );
    }

    @Test
    void deveAgruparMovimentacoesPorAcao() {
        Instant agora =
            Instant.parse(
                "2026-06-30T15:00:00Z"
            );

        Movimentacao descida =
            movimentacao(
                "30000000-0000-0000-0000-000000000001",
                TipoMovimentacao.LANCAMENTO,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.URGENTE,
                agora.minusSeconds(10 * 60),
                null,
                false
            );

        Movimentacao retirada =
            movimentacao(
                "30000000-0000-0000-0000-000000000002",
                TipoMovimentacao.RETIRADA,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.NORMAL,
                agora.plusSeconds(10 * 60),
                null,
                true
            );

        Movimentacao transferencia =
            movimentacao(
                "30000000-0000-0000-0000-000000000003",
                TipoMovimentacao.TRANSFERENCIA,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.ALTA,
                agora.plusSeconds(2 * 60 * 60),
                null,
                false
            );

        Movimentacao deslocamento =
            movimentacao(
                "30000000-0000-0000-0000-000000000004",
                TipoMovimentacao
                    .DESLOCAMENTO_INTERNO,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.NORMAL,
                agora.plusSeconds(3 * 60 * 60),
                null,
                true
            );

        Movimentacao emExecucao =
            movimentacao(
                "30000000-0000-0000-0000-000000000005",
                TipoMovimentacao.LANCAMENTO,
                StatusMovimentacao.EM_EXECUCAO,
                PrioridadeMovimentacao.ALTA,
                agora.minusSeconds(45 * 60),
                agora.minusSeconds(30 * 60),
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
                descida,
                retirada,
                transferencia,
                deslocamento,
                emExecucao
            )
        );

        PainelTvOperacionalResponse painel =
            service.buscar(agora);

        assertThat(painel.descidasParaAgua())
            .singleElement()
            .satisfies(item -> {
                assertThat(
                    item.acaoOperacional()
                ).isEqualTo(
                    AcaoOperacionalPainelTv
                        .DESCER_PARA_AGUA
                );

                assertThat(item.situacao())
                    .isEqualTo(
                        SituacaoPainelTv.ATRASADA
                    );

                assertThat(item.minutosAtraso())
                    .isEqualTo(10);

                assertThat(item.alertas())
                    .containsExactly(
                        TipoAlertaPainelTv.ATRASADA,
                        TipoAlertaPainelTv.URGENTE,
                        TipoAlertaPainelTv
                            .SEM_OPERADOR
                    );
            });

        assertThat(painel.retiradasDaAgua())
            .singleElement()
            .satisfies(item -> {
                assertThat(
                    item.acaoOperacional()
                ).isEqualTo(
                    AcaoOperacionalPainelTv
                        .RETIRAR_DA_AGUA
                );

                assertThat(item.situacao())
                    .isEqualTo(
                        SituacaoPainelTv.PROXIMA
                    );

                assertThat(
                    item.minutosParaInicio()
                ).isEqualTo(10);
            });

        assertThat(
            painel.transferenciasDeVaga()
        ).hasSize(1);

        assertThat(
            painel.deslocamentosInternos()
        ).hasSize(1);

        assertThat(painel.emExecucao())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.situacao())
                    .isEqualTo(
                        SituacaoPainelTv
                            .EM_EXECUCAO
                    );

                assertThat(
                    item.minutosEmExecucao()
                ).isEqualTo(30);
            });

        assertThat(painel.alertas())
            .extracting(
                item -> item.id()
            )
            .containsExactly(
                descida.getId(),
                retirada.getId(),
                transferencia.getId()
            );

        assertThat(
            painel.resumo().descidasParaAgua()
        ).isEqualTo(1);

        assertThat(
            painel.resumo().retiradasDaAgua()
        ).isEqualTo(1);

        assertThat(
            painel.resumo().emExecucao()
        ).isEqualTo(1);

        assertThat(
            painel.resumo().alertas()
        ).isEqualTo(3);

        assertThat(
            painel.atualizarAposSegundos()
        ).isEqualTo(15);
    }

    @Test
    void devePriorizarAtrasadaAntesDeUrgente() {
        Instant agora =
            Instant.parse(
                "2026-06-30T15:00:00Z"
            );

        Movimentacao urgenteFutura =
            movimentacao(
                "40000000-0000-0000-0000-000000000001",
                TipoMovimentacao.LANCAMENTO,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.URGENTE,
                agora.plusSeconds(10 * 60),
                null,
                true
            );

        Movimentacao normalAtrasada =
            movimentacao(
                "40000000-0000-0000-0000-000000000002",
                TipoMovimentacao.LANCAMENTO,
                StatusMovimentacao.AGENDADA,
                PrioridadeMovimentacao.NORMAL,
                agora.minusSeconds(5 * 60),
                null,
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
                urgenteFutura,
                normalAtrasada
            )
        );

        PainelTvOperacionalResponse painel =
            service.buscar(agora);

        assertThat(painel.descidasParaAgua())
            .extracting(item -> item.id())
            .containsExactly(
                normalAtrasada.getId(),
                urgenteFutura.getId()
            );
    }

    private Movimentacao movimentacao(
        String id,
        TipoMovimentacao tipo,
        StatusMovimentacao status,
        PrioridadeMovimentacao prioridade,
        Instant agendadaPara,
        Instant iniciadaEm,
        boolean possuiOperador
    ) {
        Movimentacao movimentacao =
            mock(Movimentacao.class);

        Usuario operador =
            possuiOperador
                ? mock(Usuario.class)
                : null;

        when(movimentacao.getId())
            .thenReturn(UUID.fromString(id));

        when(movimentacao.getTipo())
            .thenReturn(tipo);

        when(movimentacao.getStatus())
            .thenReturn(status);

        when(movimentacao.getPrioridade())
            .thenReturn(prioridade);

        when(movimentacao.getAgendadaPara())
            .thenReturn(agendadaPara);

        when(movimentacao.getIniciadaEm())
            .thenReturn(iniciadaEm);

        when(
            movimentacao.getOperadorResponsavel()
        ).thenReturn(operador);

        if (operador != null) {
            when(operador.getId())
                .thenReturn(
                    UUID.fromString(
                        "80000000-0000-0000-0000-000000000001"
                    )
                );

            when(operador.getNome())
                .thenReturn("Carlos");
        }

        return movimentacao;
    }

    private MovimentacaoResponse respostaBase(
        Movimentacao movimentacao
    ) {
        Usuario operador =
            movimentacao
                .getOperadorResponsavel();

        TipoMovimentacao tipo =
            movimentacao.getTipo();

        TipoPosicaoEmbarcacao origem =
            tipo == TipoMovimentacao.RETIRADA
                ? TipoPosicaoEmbarcacao.AGUA
                : TipoPosicaoEmbarcacao.VAGA;

        TipoPosicaoEmbarcacao destino =
            switch (tipo) {
                case LANCAMENTO ->
                    TipoPosicaoEmbarcacao.AGUA;
                case RETIRADA,
                     TRANSFERENCIA ->
                    TipoPosicaoEmbarcacao.VAGA;
                case DESLOCAMENTO_INTERNO ->
                    TipoPosicaoEmbarcacao
                        .AREA_SERVICO;
            };

        return new MovimentacaoResponse(
            movimentacao.getId(),
            UUID.fromString(
                "50000000-0000-0000-0000-000000000001"
            ),
            "Aurora",
            "V33",
            "João",
            tipo,
            movimentacao.getStatus(),
            movimentacao.getPrioridade(),
            origem,
            origem == TipoPosicaoEmbarcacao.VAGA
                ? UUID.fromString(
                    "60000000-0000-0000-0000-000000000001"
                )
                : null,
            origem == TipoPosicaoEmbarcacao.VAGA
                ? "A-01"
                : null,
            origem == TipoPosicaoEmbarcacao.AGUA
                ? "Canal principal"
                : null,
            destino,
            destino == TipoPosicaoEmbarcacao.VAGA
                ? UUID.fromString(
                    "60000000-0000-0000-0000-000000000002"
                )
                : null,
            destino == TipoPosicaoEmbarcacao.VAGA
                ? "B-02"
                : null,
            destino != TipoPosicaoEmbarcacao.VAGA
                ? "Rampa principal"
                : null,
            movimentacao.getAgendadaPara(),
            movimentacao.getIniciadaEm(),
            null,
            null,
            UUID.fromString(
                "70000000-0000-0000-0000-000000000001"
            ),
            "Administrador",
            operador == null
                ? null
                : operador.getId(),
            operador == null
                ? null
                : operador.getNome(),
            null,
            null,
            0L,
            ORGANIZACAO_ID,
            movimentacao
                .getAgendadaPara()
                .minusSeconds(60 * 60),
            movimentacao.getAgendadaPara()
        );
    }
}
