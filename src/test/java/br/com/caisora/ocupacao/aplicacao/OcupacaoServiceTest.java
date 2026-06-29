package br.com.caisora.ocupacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.ocupacao.api.AtualizarOcupacaoRequest;
import br.com.caisora.ocupacao.api.CriarOcupacaoRequest;
import br.com.caisora.ocupacao.api.EncerrarOcupacaoRequest;
import br.com.caisora.ocupacao.api.OcupacaoResponse;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OcupacaoServiceTest {

    @Mock
    private OcupacaoRepository ocupacaoRepository;

    @Mock
    private EmbarcacaoRepository embarcacaoRepository;

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private OcupacaoService ocupacaoService;

    @BeforeEach
    void configurar() {
        ocupacaoService = new OcupacaoService(
            ocupacaoRepository,
            embarcacaoRepository,
            vagaRepository,
            new OcupacaoMapper(),
            leitorTokenJwt
        );
    }

    @Test
    void deveCriarOcupacaoComDadosValidos() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario =
            criarClientePersistido(
                UUID.randomUUID(),
                organizacao
            );

        Embarcacao embarcacao =
            criarEmbarcacaoPersistida(
                embarcacaoId,
                organizacao,
                proprietario,
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("1.00"),
                new BigDecimal("3.00"),
                new BigDecimal("5000.00")
            );

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            new BigDecimal("12.00"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("4.00"),
            new BigDecimal("7000.00")
        );

        Instant inicio =
            Instant.now().minusSeconds(300);

        Instant fimPrevisto =
            inicio.plusSeconds(86400);

        prepararUsuarioAutenticado(organizacaoId);

        when(
            embarcacaoRepository
                .findByIdAndOrganizacaoId(
                    embarcacaoId,
                    organizacaoId
                )
        ).thenReturn(Optional.of(embarcacao));

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        when(
            ocupacaoRepository.save(
                any(Ocupacao.class)
            )
        ).thenAnswer(invocacao -> {
            Ocupacao ocupacao =
                invocacao.getArgument(0);

            popularAuditoria(
                ocupacao,
                UUID.randomUUID()
            );

            return ocupacao;
        });

        OcupacaoResponse response =
            ocupacaoService.criar(
                new CriarOcupacaoRequest(
                    embarcacaoId,
                    vagaId,
                    inicio,
                    fimPrevisto,
                    " Ocupação mensal "
                )
            );

        ArgumentCaptor<Ocupacao> captor =
            ArgumentCaptor.forClass(
                Ocupacao.class
            );

        verify(ocupacaoRepository)
            .save(captor.capture());

        Ocupacao salva = captor.getValue();

        assertThat(salva.getEmbarcacao().getId())
            .isEqualTo(embarcacaoId);

        assertThat(salva.getVaga().getId())
            .isEqualTo(vagaId);

        assertThat(salva.getStatus())
            .isEqualTo(StatusOcupacao.ATIVA);

        assertThat(salva.getInicioEm())
            .isEqualTo(inicio);

        assertThat(salva.getFimPrevistoEm())
            .isEqualTo(fimPrevisto);

        assertThat(salva.getObservacoes())
            .isEqualTo("Ocupação mensal");

        assertThat(response.embarcacaoId())
            .isEqualTo(embarcacaoId);

        assertThat(response.vagaId())
            .isEqualTo(vagaId);

        assertThat(response.organizacaoId())
            .isEqualTo(organizacaoId);

        assertThat(response.status())
            .isEqualTo(StatusOcupacao.ATIVA);
    }

    @Test
    void deveFalharAoCriarComEmbarcacaoInexistente() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        prepararUsuarioAutenticado(organizacaoId);

        when(
            embarcacaoRepository
                .findByIdAndOrganizacaoId(
                    embarcacaoId,
                    organizacaoId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    embarcacaoId,
                    UUID.randomUUID()
                )
            )
        )
            .isInstanceOf(
                RecursoNaoEncontradoException.class
            )
            .hasMessage(
                "Embarcacao nao encontrada"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharAoCriarComVagaInexistente() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario =
            criarClientePersistido(
                UUID.randomUUID(),
                organizacao
            );

        Embarcacao embarcacao =
            criarEmbarcacaoPersistidaPadrao(
                embarcacaoId,
                organizacao,
                proprietario
            );

        prepararUsuarioAutenticado(organizacaoId);

        when(
            embarcacaoRepository
                .findByIdAndOrganizacaoId(
                    embarcacaoId,
                    organizacaoId
                )
        ).thenReturn(Optional.of(embarcacao));

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    embarcacaoId,
                    vagaId
                )
            )
        )
            .isInstanceOf(
                RecursoNaoEncontradoException.class
            )
            .hasMessage("Vaga nao encontrada");

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharAoCriarComEmbarcacaoInativa() {
        Contexto contexto = criarContextoPadrao();

        contexto.embarcacao().inativar();

        prepararCriacao(contexto);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                ConflitoDadosException.class
            )
            .hasMessage(
                "Nao e possivel ocupar uma vaga "
                    + "com uma embarcacao inativa"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharAoCriarComVagaInativa() {
        Contexto contexto = criarContextoPadrao();

        contexto.vaga().inativar();

        prepararCriacao(contexto);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                ConflitoDadosException.class
            )
            .hasMessage(
                "Nao e possivel criar uma ocupacao "
                    + "em uma vaga inativa"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharQuandoEmbarcacaoJaPossuiOcupacaoAtiva() {
        Contexto contexto = criarContextoPadrao();

        prepararCriacao(contexto);

        when(
            ocupacaoRepository
                .existsByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                    contexto.organizacao().getId(),
                    contexto.embarcacao().getId(),
                    StatusOcupacao.ATIVA
                )
        ).thenReturn(true);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                ConflitoDadosException.class
            )
            .hasMessage(
                "A embarcacao ja possui uma "
                    + "ocupacao ativa"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharQuandoVagaJaPossuiOcupacaoAtiva() {
        Contexto contexto = criarContextoPadrao();

        prepararCriacao(contexto);

        when(
            ocupacaoRepository
                .existsByOrganizacaoIdAndVagaIdAndStatus(
                    contexto.organizacao().getId(),
                    contexto.vaga().getId(),
                    StatusOcupacao.ATIVA
                )
        ).thenReturn(true);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                ConflitoDadosException.class
            )
            .hasMessage(
                "A vaga ja possui uma "
                    + "ocupacao ativa"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharQuandoComprimentoExcedeLimiteDaVaga() {
        Contexto contexto =
            criarContexto(
                new BigDecimal("13.00"),
                new BigDecimal("3.00"),
                new BigDecimal("1.00"),
                new BigDecimal("3.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("4.00"),
                new BigDecimal("1.50"),
                new BigDecimal("4.00"),
                new BigDecimal("7000.00")
            );

        prepararCriacao(contexto);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "O comprimento da embarcacao "
                    + "excede o limite da vaga"
            );

        verify(ocupacaoRepository, never())
            .save(any(Ocupacao.class));
    }

    @Test
    void deveFalharQuandoBocaExcedeLimiteDaVaga() {
        Contexto contexto =
            criarContexto(
                new BigDecimal("10.00"),
                new BigDecimal("4.50"),
                new BigDecimal("1.00"),
                new BigDecimal("3.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("4.00"),
                new BigDecimal("1.50"),
                new BigDecimal("4.00"),
                new BigDecimal("7000.00")
            );

        prepararCriacao(contexto);

        assertThatThrownBy(
            () -> ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            )
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "A boca da embarcacao "
                    + "excede o limite da vaga"
            );
    }

    @Test
    void deveIgnorarLimiteOpcionalNaoInformadoNaVaga() {
        Contexto contexto =
            criarContexto(
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("2.00"),
                new BigDecimal("8.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("4.00"),
                null,
                null,
                null
            );

        prepararCriacao(contexto);

        when(
            ocupacaoRepository.save(
                any(Ocupacao.class)
            )
        ).thenAnswer(invocacao -> {
            Ocupacao ocupacao =
                invocacao.getArgument(0);

            popularAuditoria(
                ocupacao,
                UUID.randomUUID()
            );

            return ocupacao;
        });

        OcupacaoResponse response =
            ocupacaoService.criar(
                criarRequest(
                    contexto.embarcacao().getId(),
                    contexto.vaga().getId()
                )
            );

        assertThat(response.status())
            .isEqualTo(StatusOcupacao.ATIVA);
    }

    @Test
    void deveFalharComInicioNoFuturo() {
        UUID organizacaoId = UUID.randomUUID();

        prepararUsuarioAutenticado(organizacaoId);

        CriarOcupacaoRequest request =
            new CriarOcupacaoRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                null,
                null
            );

        assertThatThrownBy(
            () -> ocupacaoService.criar(request)
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "O inicio da ocupacao nao pode "
                    + "estar no futuro"
            );

        verify(embarcacaoRepository, never())
            .findByIdAndOrganizacaoId(
                any(UUID.class),
                any(UUID.class)
            );
    }

    @Test
    void deveFalharComFimPrevistoAnteriorAoInicio() {
        UUID organizacaoId = UUID.randomUUID();

        prepararUsuarioAutenticado(organizacaoId);

        Instant inicio =
            Instant.now().minusSeconds(300);

        CriarOcupacaoRequest request =
            new CriarOcupacaoRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                inicio,
                inicio.minusSeconds(1),
                null
            );

        assertThatThrownBy(
            () -> ocupacaoService.criar(request)
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "O fim previsto deve ser posterior "
                    + "ao inicio da ocupacao"
            );
    }

    @Test
    void deveListarSomenteOcupacoesDaOrganizacao() {
        Contexto contexto = criarContextoPadrao();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                UUID.randomUUID(),
                contexto,
                StatusOcupacao.ATIVA
            );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findAllByOrganizacaoId(
                    contexto.organizacao().getId(),
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(ocupacao),
                paginacao,
                1
            )
        );

        var pagina =
            ocupacaoService.listar(paginacao);

        assertThat(pagina.getContent())
            .hasSize(1);

        assertThat(
            pagina.getContent()
                .get(0)
                .organizacaoId()
        ).isEqualTo(
            contexto.organizacao().getId()
        );
    }

    @Test
    void deveListarPorStatus() {
        Contexto contexto = criarContextoPadrao();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                UUID.randomUUID(),
                contexto,
                StatusOcupacao.ATIVA
            );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findAllByOrganizacaoIdAndStatus(
                    contexto.organizacao().getId(),
                    StatusOcupacao.ATIVA,
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(ocupacao),
                paginacao,
                1
            )
        );

        var pagina =
            ocupacaoService.listarPorStatus(
                StatusOcupacao.ATIVA,
                paginacao
            );

        assertThat(pagina.getContent())
            .extracting(OcupacaoResponse::status)
            .containsExactly(
                StatusOcupacao.ATIVA
            );
    }

    @Test
    void deveListarHistoricoPorEmbarcacao() {
        Contexto contexto = criarContextoPadrao();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                UUID.randomUUID(),
                contexto,
                StatusOcupacao.ATIVA
            );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findAllByOrganizacaoIdAndEmbarcacaoId(
                    contexto.organizacao().getId(),
                    contexto.embarcacao().getId(),
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(ocupacao),
                paginacao,
                1
            )
        );

        var pagina =
            ocupacaoService.listarPorEmbarcacao(
                contexto.embarcacao().getId(),
                paginacao
            );

        assertThat(pagina.getContent())
            .extracting(
                OcupacaoResponse::embarcacaoId
            )
            .containsExactly(
                contexto.embarcacao().getId()
            );
    }

    @Test
    void deveListarHistoricoPorVaga() {
        Contexto contexto = criarContextoPadrao();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                UUID.randomUUID(),
                contexto,
                StatusOcupacao.ATIVA
            );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findAllByOrganizacaoIdAndVagaId(
                    contexto.organizacao().getId(),
                    contexto.vaga().getId(),
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(ocupacao),
                paginacao,
                1
            )
        );

        var pagina =
            ocupacaoService.listarPorVaga(
                contexto.vaga().getId(),
                paginacao
            );

        assertThat(pagina.getContent())
            .extracting(OcupacaoResponse::vagaId)
            .containsExactly(
                contexto.vaga().getId()
            );
    }

    @Test
    void deveBuscarOcupacaoPorIdNaOrganizacao() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ATIVA
            );

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        OcupacaoResponse response =
            ocupacaoService.buscarPorId(
                ocupacaoId
            );

        assertThat(response.id())
            .isEqualTo(ocupacaoId);

        assertThat(response.organizacaoId())
            .isEqualTo(
                contexto.organizacao().getId()
            );
    }

    @Test
    void deveFalharAoBuscarOcupacaoDeOutraOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID ocupacaoId = UUID.randomUUID();

        prepararUsuarioAutenticado(organizacaoId);

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    organizacaoId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> ocupacaoService.buscarPorId(
                ocupacaoId
            )
        )
            .isInstanceOf(
                RecursoNaoEncontradoException.class
            )
            .hasMessage(
                "Ocupacao nao encontrada"
            );
    }

    @Test
    void deveAtualizarFimPrevistoEObservacoes() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ATIVA
            );

        Instant novoFim =
            ocupacao.getInicioEm()
                .plusSeconds(172800);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        when(ocupacaoRepository.save(ocupacao))
            .thenReturn(ocupacao);

        OcupacaoResponse response =
            ocupacaoService.atualizar(
                ocupacaoId,
                new AtualizarOcupacaoRequest(
                    novoFim,
                    " Nova previsão "
                )
            );

        assertThat(response.fimPrevistoEm())
            .isEqualTo(novoFim);

        assertThat(response.observacoes())
            .isEqualTo("Nova previsão");

        assertThat(response.embarcacaoId())
            .isEqualTo(
                contexto.embarcacao().getId()
            );

        assertThat(response.vagaId())
            .isEqualTo(
                contexto.vaga().getId()
            );
    }

    @Test
    void naoDeveAtualizarOcupacaoEncerrada() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ENCERRADA
            );

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        assertThatThrownBy(
            () -> ocupacaoService.atualizar(
                ocupacaoId,
                new AtualizarOcupacaoRequest(
                    null,
                    null
                )
            )
        )
            .isInstanceOf(
                ConflitoDadosException.class
            )
            .hasMessage(
                "A ocupacao ja esta encerrada"
            );

        verify(ocupacaoRepository, never())
            .save(ocupacao);
    }

    @Test
    void deveEncerrarOcupacaoAtiva() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ATIVA
            );

        Instant encerramento =
            Instant.now().minusSeconds(30);

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        when(ocupacaoRepository.save(ocupacao))
            .thenReturn(ocupacao);

        OcupacaoResponse response =
            ocupacaoService.encerrar(
                ocupacaoId,
                new EncerrarOcupacaoRequest(
                    encerramento
                )
            );

        assertThat(response.status())
            .isEqualTo(
                StatusOcupacao.ENCERRADA
            );

        assertThat(response.encerradaEm())
            .isEqualTo(encerramento);

        assertThat(ocupacao.estaAtiva())
            .isFalse();
    }

    @Test
    void deveFalharAoEncerrarAntesDoInicio() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ATIVA
            );

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        assertThatThrownBy(
            () -> ocupacaoService.encerrar(
                ocupacaoId,
                new EncerrarOcupacaoRequest(
                    ocupacao.getInicioEm()
                        .minusSeconds(1)
                )
            )
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "O encerramento nao pode ser "
                    + "anterior ao inicio da ocupacao"
            );

        verify(ocupacaoRepository, never())
            .save(ocupacao);
    }

    @Test
    void deveFalharAoEncerrarNoFuturo() {
        Contexto contexto = criarContextoPadrao();
        UUID ocupacaoId = UUID.randomUUID();

        Ocupacao ocupacao =
            criarOcupacaoPersistida(
                ocupacaoId,
                contexto,
                StatusOcupacao.ATIVA
            );

        prepararUsuarioAutenticado(
            contexto.organizacao().getId()
        );

        when(
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    ocupacaoId,
                    contexto.organizacao().getId()
                )
        ).thenReturn(Optional.of(ocupacao));

        assertThatThrownBy(
            () -> ocupacaoService.encerrar(
                ocupacaoId,
                new EncerrarOcupacaoRequest(
                    Instant.now()
                        .plusSeconds(3600)
                )
            )
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "O encerramento nao pode "
                    + "estar no futuro"
            );
    }

    @Test
    void deveFalharAoEncerrarComDataNula() {
        assertThatThrownBy(
            () -> ocupacaoService.encerrar(
                UUID.randomUUID(),
                new EncerrarOcupacaoRequest(null)
            )
        )
            .isInstanceOf(
                DadosInvalidosException.class
            )
            .hasMessage(
                "Data de encerramento obrigatoria"
            );

        verify(ocupacaoRepository, never())
            .findByIdAndOrganizacaoId(
                any(UUID.class),
                any(UUID.class)
            );
    }

    private CriarOcupacaoRequest criarRequest(
        UUID embarcacaoId,
        UUID vagaId
    ) {
        Instant inicio =
            Instant.now().minusSeconds(300);

        return new CriarOcupacaoRequest(
            embarcacaoId,
            vagaId,
            inicio,
            inicio.plusSeconds(86400),
            "Ocupação de teste"
        );
    }

    private void prepararCriacao(
        Contexto contexto
    ) {
        UUID organizacaoId =
            contexto.organizacao().getId();

        prepararUsuarioAutenticado(
            organizacaoId
        );

        when(
            embarcacaoRepository
                .findByIdAndOrganizacaoId(
                    contexto.embarcacao().getId(),
                    organizacaoId
                )
        ).thenReturn(
            Optional.of(
                contexto.embarcacao()
            )
        );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                contexto.vaga().getId(),
                organizacaoId
            )
        ).thenReturn(
            Optional.of(contexto.vaga())
        );
    }

    private void prepararUsuarioAutenticado(
        UUID organizacaoId
    ) {
        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(
                    organizacaoId
                )
            );
    }

    private Contexto criarContextoPadrao() {
        return criarContexto(
            new BigDecimal("10.00"),
            new BigDecimal("3.00"),
            new BigDecimal("1.00"),
            new BigDecimal("3.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("12.00"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("4.00"),
            new BigDecimal("7000.00")
        );
    }

    private Contexto criarContexto(
        BigDecimal comprimentoEmbarcacao,
        BigDecimal bocaEmbarcacao,
        BigDecimal caladoEmbarcacao,
        BigDecimal alturaEmbarcacao,
        BigDecimal pesoEmbarcacao,
        BigDecimal comprimentoVaga,
        BigDecimal bocaVaga,
        BigDecimal caladoVaga,
        BigDecimal alturaVaga,
        BigDecimal pesoVaga
    ) {
        Organizacao organizacao =
            criarOrganizacaoPersistida(
                UUID.randomUUID()
            );

        Cliente proprietario =
            criarClientePersistido(
                UUID.randomUUID(),
                organizacao
            );

        Embarcacao embarcacao =
            criarEmbarcacaoPersistida(
                UUID.randomUUID(),
                organizacao,
                proprietario,
                comprimentoEmbarcacao,
                bocaEmbarcacao,
                caladoEmbarcacao,
                alturaEmbarcacao,
                pesoEmbarcacao
            );

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            comprimentoVaga,
            bocaVaga,
            caladoVaga,
            alturaVaga,
            pesoVaga
        );

        return new Contexto(
            organizacao,
            proprietario,
            embarcacao,
            vaga
        );
    }

    private Organizacao criarOrganizacaoPersistida(
        UUID id
    ) {
        Organizacao organizacao =
            Organizacao.criar(
                "Marina Teste",
                "marina-teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999"
            );

        ReflectionTestUtils.setField(
            organizacao,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            organizacao,
            "criadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        ReflectionTestUtils.setField(
            organizacao,
            "atualizadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        return organizacao;
    }

    private Cliente criarClientePersistido(
        UUID id,
        Organizacao organizacao
    ) {
        Cliente cliente = new Cliente(
            organizacao,
            TipoPessoa.FISICA,
            "Joao da Silva",
            null,
            "52998224725",
            "joao@email.com",
            "4133334444",
            "41999998888",
            "Cliente mensalista"
        );

        ReflectionTestUtils.setField(
            cliente,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            cliente,
            "criadoEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        ReflectionTestUtils.setField(
            cliente,
            "atualizadoEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        return cliente;
    }

    private Embarcacao
    criarEmbarcacaoPersistidaPadrao(
        UUID id,
        Organizacao organizacao,
        Cliente proprietario
    ) {
        return criarEmbarcacaoPersistida(
            id,
            organizacao,
            proprietario,
            new BigDecimal("10.00"),
            new BigDecimal("3.00"),
            new BigDecimal("1.00"),
            new BigDecimal("3.00"),
            new BigDecimal("5000.00")
        );
    }

    private Embarcacao criarEmbarcacaoPersistida(
        UUID id,
        Organizacao organizacao,
        Cliente proprietario,
        BigDecimal comprimento,
        BigDecimal boca,
        BigDecimal calado,
        BigDecimal altura,
        BigDecimal peso
    ) {
        Embarcacao embarcacao =
            new Embarcacao(
                organizacao,
                proprietario,
                "Aurora",
                TipoEmbarcacao.LANCHA,
                "Schaefer",
                "V33",
                2023,
                "PR-123456",
                "BR-SCH12345A323",
                "Paranagua",
                "BR",
                comprimento,
                boca,
                calado,
                new BigDecimal("1.70"),
                altura,
                peso,
                12,
                TipoPropulsao.MOTOR,
                "Branca",
                "Embarcacao principal"
            );

        ReflectionTestUtils.setField(
            embarcacao,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            embarcacao,
            "criadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        ReflectionTestUtils.setField(
            embarcacao,
            "atualizadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        return embarcacao;
    }

    private Vaga criarVagaPersistida(
        UUID id,
        Organizacao organizacao,
        BigDecimal comprimento,
        BigDecimal boca,
        BigDecimal calado,
        BigDecimal altura,
        BigDecimal peso
    ) {
        Vaga vaga = new Vaga(
            organizacao,
            "A-01",
            TipoVaga.MOLHADA,
            "Pier A",
            "Corredor principal",
            comprimento,
            boca,
            calado,
            altura,
            peso,
            true,
            true,
            "Vaga principal"
        );

        ReflectionTestUtils.setField(
            vaga,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            vaga,
            "criadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        ReflectionTestUtils.setField(
            vaga,
            "atualizadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        return vaga;
    }

    private Ocupacao criarOcupacaoPersistida(
        UUID id,
        Contexto contexto,
        StatusOcupacao status
    ) {
        Instant inicio =
            Instant.now().minusSeconds(3600);

        Ocupacao ocupacao = new Ocupacao(
            contexto.organizacao(),
            contexto.embarcacao(),
            contexto.vaga(),
            inicio,
            inicio.plusSeconds(86400),
            "Ocupação de teste"
        );

        if (
            status
                == StatusOcupacao.ENCERRADA
        ) {
            ocupacao.encerrar(
                inicio.plusSeconds(1800)
            );
        }

        popularAuditoria(ocupacao, id);

        return ocupacao;
    }

    private void popularAuditoria(
        Ocupacao ocupacao,
        UUID id
    ) {
        ReflectionTestUtils.setField(
            ocupacao,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            ocupacao,
            "criadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );

        ReflectionTestUtils.setField(
            ocupacao,
            "atualizadaEm",
            Instant.parse(
                "2026-06-29T20:00:00Z"
            )
        );
    }

    private UsuarioAutenticado
    criarUsuarioAutenticado(
        UUID organizacaoId
    ) {
        return new UsuarioAutenticado(
            UUID.randomUUID(),
            "Administrador",
            "admin@marina.com",
            PerfilUsuario.ADMINISTRADOR_MARINA,
            organizacaoId,
            "Marina Teste"
        );
    }

    private record Contexto(
        Organizacao organizacao,
        Cliente proprietario,
        Embarcacao embarcacao,
        Vaga vaga
    ) {
    }
}
