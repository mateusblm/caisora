package br.com.caisora.movimentacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.movimentacao.api.CancelarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.ConcluirMovimentacaoRequest;
import br.com.caisora.movimentacao.api.CriarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.IniciarMovimentacaoRequest;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacao;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacaoRepository;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;

@ExtendWith(MockitoExtension.class)
class MovimentacaoServiceTest {

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @Mock
    private PosicaoEmbarcacaoRepository posicaoRepository;

    @Mock
    private HistoricoMovimentacaoRepository historicoRepository;

    @Mock
    private EmbarcacaoRepository embarcacaoRepository;

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OcupacaoRepository ocupacaoRepository;

    @Mock
    private PosicaoEmbarcacaoService posicaoService;

    @Mock
    private MovimentacaoMapper movimentacaoMapper;

    @Mock
    private HistoricoMovimentacaoMapper historicoMapper;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private MovimentacaoService service;

    private UUID organizacaoId;
    private UUID usuarioId;
    private UUID embarcacaoId;
    private UUID movimentacaoId;

    private Organizacao organizacao;
    private Usuario usuario;
    private Embarcacao embarcacao;
    private Vaga vagaOrigem;
    private Vaga vagaDestino;

    @BeforeEach
    void configurar() {
        service = new MovimentacaoService(
                movimentacaoRepository,
                posicaoRepository,
                historicoRepository,
                embarcacaoRepository,
                vagaRepository,
                usuarioRepository,
                ocupacaoRepository,
                posicaoService,
                movimentacaoMapper,
                historicoMapper,
                leitorTokenJwt);

        organizacaoId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        embarcacaoId = UUID.randomUUID();
        movimentacaoId = UUID.randomUUID();

        organizacao = org.mockito.Mockito.mock(Organizacao.class);
        usuario = org.mockito.Mockito.mock(Usuario.class);
        embarcacao = org.mockito.Mockito.mock(Embarcacao.class);
        vagaOrigem = org.mockito.Mockito.mock(Vaga.class);
        vagaDestino = org.mockito.Mockito.mock(Vaga.class);

        lenient().when(usuario.getId()).thenReturn(usuarioId);
        lenient().when(usuario.isAtivo()).thenReturn(true);
        lenient().when(embarcacao.getId()).thenReturn(embarcacaoId);
        lenient().when(embarcacao.getOrganizacao()).thenReturn(organizacao);
        lenient().when(embarcacao.isAtiva()).thenReturn(true);
        lenient().when(vagaOrigem.getId()).thenReturn(UUID.randomUUID());
        lenient().when(vagaDestino.getId()).thenReturn(UUID.randomUUID());
        lenient().when(vagaOrigem.isAtiva()).thenReturn(true);
        lenient().when(vagaDestino.isAtiva()).thenReturn(true);
    }

    @Test
    void deveCriarLancamentoValido() {
        Instant agendadaPara = Instant.now().plusSeconds(3600);
        CriarMovimentacaoRequest request = new CriarMovimentacaoRequest(
                embarcacaoId,
                TipoMovimentacao.LANCAMENTO,
                PrioridadeMovimentacao.ALTA,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                "Canal principal",
                agendadaPara,
                null,
                "Cliente presente na marina");

        PosicaoEmbarcacao posicao = PosicaoEmbarcacao.criarEmVaga(organizacao, embarcacao, vagaOrigem);
        Ocupacao ocupacao = org.mockito.Mockito.mock(Ocupacao.class);
        when(ocupacao.getVaga()).thenReturn(vagaOrigem);

        prepararAutenticacao();
        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.of(embarcacao));
        when(movimentacaoRepository.existsByOrganizacaoIdAndEmbarcacaoIdAndStatusIn(
                eq(organizacaoId),
                eq(embarcacaoId),
                any()))
                .thenReturn(false);
        when(posicaoService.obterOuCriarPosicao(embarcacao, organizacaoId)).thenReturn(posicao);
        when(ocupacaoRepository.findByOrganizacaoIdAndEmbarcacaoIdAndStatus(organizacaoId, embarcacaoId, StatusOcupacao.ATIVA))
                .thenReturn(Optional.of(ocupacao));
        when(movimentacaoRepository.save(any(Movimentacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        service.criar(request);

        ArgumentCaptor<Movimentacao> movimentacaoCaptor = ArgumentCaptor.forClass(Movimentacao.class);
        verify(movimentacaoRepository).save(movimentacaoCaptor.capture());
        verify(movimentacaoRepository).flush();
        verify(historicoRepository).save(any(HistoricoMovimentacao.class));

        Movimentacao criada = movimentacaoCaptor.getValue();
        assertThat(criada.getTipo()).isEqualTo(TipoMovimentacao.LANCAMENTO);
        assertThat(criada.getStatus()).isEqualTo(StatusMovimentacao.AGENDADA);
        assertThat(criada.getTipoPosicaoOrigem()).isEqualTo(TipoPosicaoEmbarcacao.VAGA);
        assertThat(criada.getVagaOrigem()).isSameAs(vagaOrigem);
        assertThat(criada.getTipoPosicaoDestino()).isEqualTo(TipoPosicaoEmbarcacao.AGUA);
        assertThat(criada.getVagaDestino()).isNull();
        assertThat(criada.getSolicitadaPor()).isSameAs(usuario);
        assertThat(criada.getAgendadaPara()).isEqualTo(agendadaPara);
    }

    @Test
    void naoDeveCriarSegundaMovimentacaoAbertaParaMesmaEmbarcacao() {
        CriarMovimentacaoRequest request = new CriarMovimentacaoRequest(
                embarcacaoId,
                TipoMovimentacao.LANCAMENTO,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                null,
                Instant.now().plusSeconds(3600),
                null,
                null);

        prepararAutenticacao();
        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.of(embarcacao));
        when(movimentacaoRepository.existsByOrganizacaoIdAndEmbarcacaoIdAndStatusIn(
                eq(organizacaoId),
                eq(embarcacaoId),
                any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflitoDadosException.class);

        verify(posicaoService, never()).obterOuCriarPosicao(any(Embarcacao.class), any(UUID.class));
        verify(movimentacaoRepository, never()).save(any(Movimentacao.class));
    }

    @Test
    void deveIniciarMovimentacaoAgendada() {
        Movimentacao movimentacao = criarMovimentacaoLancamento();
        Instant iniciadaEm = Instant.now();

        prepararAutenticacao();
        when(movimentacaoRepository.findByIdAndOrganizacaoId(movimentacaoId, organizacaoId))
                .thenReturn(Optional.of(movimentacao));
        when(movimentacaoRepository.save(movimentacao)).thenReturn(movimentacao);

        service.iniciar(movimentacaoId, new IniciarMovimentacaoRequest(iniciadaEm, "Operação iniciada"));

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.EM_EXECUCAO);
        assertThat(movimentacao.getOperadorResponsavel()).isSameAs(usuario);
        assertThat(movimentacao.getIniciadaEm()).isEqualTo(iniciadaEm);
        verify(movimentacaoRepository).flush();
        verify(historicoRepository).save(any(HistoricoMovimentacao.class));
    }

    @Test
    void naoDeveCancelarMovimentacaoEmExecucao() {
        Movimentacao movimentacao = criarMovimentacaoLancamento();
        movimentacao.iniciar(usuario, movimentacao.getCriadaEm());

        prepararAutenticacao();
        when(movimentacaoRepository.findByIdAndOrganizacaoId(movimentacaoId, organizacaoId))
                .thenReturn(Optional.of(movimentacao));

        CancelarMovimentacaoRequest request = new CancelarMovimentacaoRequest(
                Instant.now(),
                "Cancelamento após início");

        assertThatThrownBy(() -> service.cancelar(movimentacaoId, request))
                .isInstanceOf(DadosInvalidosException.class);

        verify(movimentacaoRepository, never()).save(any(Movimentacao.class));
        verify(historicoRepository, never()).save(any(HistoricoMovimentacao.class));
    }

    @Test
    void deveConcluirLancamentoEAtualizarPosicaoParaAgua() {
        Movimentacao movimentacao = criarMovimentacaoLancamento();
        movimentacao.iniciar(usuario, movimentacao.getCriadaEm());
        PosicaoEmbarcacao posicao = PosicaoEmbarcacao.criarEmVaga(organizacao, embarcacao, vagaOrigem);
        Instant concluidaEm = movimentacao.getIniciadaEm().plusSeconds(30);

        prepararAutenticacao();
        when(movimentacaoRepository.findByIdAndOrganizacaoId(movimentacaoId, organizacaoId))
                .thenReturn(Optional.of(movimentacao));
        when(posicaoRepository.findByOrganizacaoIdAndEmbarcacaoId(organizacaoId, embarcacaoId))
                .thenReturn(Optional.of(posicao));
        when(movimentacaoRepository.save(movimentacao)).thenReturn(movimentacao);
        when(posicaoRepository.save(posicao)).thenReturn(posicao);

        service.concluir(
                movimentacaoId,
                new ConcluirMovimentacaoRequest(concluidaEm, "Embarcação lançada"));

        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.CONCLUIDA);
        assertThat(posicao.getTipo()).isEqualTo(TipoPosicaoEmbarcacao.AGUA);
        assertThat(posicao.getVaga()).isNull();
        assertThat(posicao.getMovimentacaoOrigem()).isSameAs(movimentacao);
        verify(posicaoRepository).save(posicao);
        verify(historicoRepository).save(any(HistoricoMovimentacao.class));
    }

    @Test
    void deveConcluirTransferenciaPreservandoFimPrevistoDaOcupacao() {
        Instant fimPrevisto = Instant.now().plusSeconds(86400 * 30L);
        Movimentacao movimentacao = criarMovimentacaoTransferencia();
        movimentacao.iniciar(usuario, movimentacao.getCriadaEm());
        PosicaoEmbarcacao posicao = PosicaoEmbarcacao.criarEmVaga(organizacao, embarcacao, vagaOrigem);
        Ocupacao ocupacaoAtual = org.mockito.Mockito.mock(Ocupacao.class);
        Instant concluidaEm = movimentacao.getIniciadaEm().plusSeconds(30);

        when(ocupacaoAtual.getVaga()).thenReturn(vagaOrigem);
        when(ocupacaoAtual.getFimPrevistoEm()).thenReturn(fimPrevisto);
        when(ocupacaoAtual.getObservacoes()).thenReturn("Ocupação original");

        prepararAutenticacao();
        when(movimentacaoRepository.findByIdAndOrganizacaoId(movimentacaoId, organizacaoId))
                .thenReturn(Optional.of(movimentacao));
        when(posicaoRepository.findByOrganizacaoIdAndEmbarcacaoId(organizacaoId, embarcacaoId))
                .thenReturn(Optional.of(posicao));
        when(ocupacaoRepository.findByOrganizacaoIdAndVagaIdAndStatus(organizacaoId, vagaDestino.getId(), StatusOcupacao.ATIVA))
                .thenReturn(Optional.empty());
        when(ocupacaoRepository.findByOrganizacaoIdAndEmbarcacaoIdAndStatus(organizacaoId, embarcacaoId, StatusOcupacao.ATIVA))
                .thenReturn(Optional.of(ocupacaoAtual));
        when(ocupacaoRepository.save(any(Ocupacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(posicaoRepository.save(posicao)).thenReturn(posicao);
        when(movimentacaoRepository.save(movimentacao)).thenReturn(movimentacao);

        service.concluir(
                movimentacaoId,
                new ConcluirMovimentacaoRequest(concluidaEm, "Transferência concluída"));

        verify(ocupacaoAtual).encerrar(concluidaEm);
        verify(ocupacaoRepository).flush();

        ArgumentCaptor<Ocupacao> ocupacaoCaptor = ArgumentCaptor.forClass(Ocupacao.class);
        verify(ocupacaoRepository, org.mockito.Mockito.times(2)).save(ocupacaoCaptor.capture());
        List<Ocupacao> ocupacoesSalvas = ocupacaoCaptor.getAllValues();
        Ocupacao novaOcupacao = ocupacoesSalvas.get(1);

        assertThat(novaOcupacao.getVaga()).isSameAs(vagaDestino);
        assertThat(novaOcupacao.getEmbarcacao()).isSameAs(embarcacao);
        assertThat(novaOcupacao.getFimPrevistoEm()).isEqualTo(fimPrevisto);
        assertThat(novaOcupacao.getStatus()).isEqualTo(StatusOcupacao.ATIVA);
        assertThat(posicao.getTipo()).isEqualTo(TipoPosicaoEmbarcacao.VAGA);
        assertThat(posicao.getVaga()).isSameAs(vagaDestino);
        assertThat(movimentacao.getStatus()).isEqualTo(StatusMovimentacao.CONCLUIDA);
    }

    @Test
    void naoDeveCombinarMaisDeUmFiltroPrincipalNaListagem() {
        assertThatThrownBy(() -> service.listar(
                StatusMovimentacao.AGENDADA,
                TipoMovimentacao.LANCAMENTO,
                null,
                null,
                null,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(DadosInvalidosException.class);

        verify(movimentacaoRepository, never())
                .findAllByOrganizacaoId(any(UUID.class), any());
    }

    private void prepararAutenticacao() {
        prepararAutenticacaoSemUsuarioPersistido();
        when(usuarioRepository.findByIdAndOrganizacaoId(usuarioId, organizacaoId))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararAutenticacaoSemUsuarioPersistido() {
        when(leitorTokenJwt.obterUsuarioAutenticado()).thenReturn(new UsuarioAutenticado(
                usuarioId,
                "Operador",
                "operador@caisora.com.br",
                PerfilUsuario.ADMINISTRADOR_MARINA,
                organizacaoId,
                "Marina Caisora"));
    }

    private Movimentacao criarMovimentacaoLancamento() {
        Movimentacao movimentacao = new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.LANCAMENTO,
                PrioridadeMovimentacao.NORMAL,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                "Canal principal",
                Instant.now().plusSeconds(3600),
                usuario,
                null,
                null);
        definirId(movimentacao, movimentacaoId);
        return movimentacao;
    }

    private Movimentacao criarMovimentacaoTransferencia() {
        Movimentacao movimentacao = new Movimentacao(
                organizacao,
                embarcacao,
                TipoMovimentacao.TRANSFERENCIA,
                PrioridadeMovimentacao.ALTA,
                TipoPosicaoEmbarcacao.VAGA,
                vagaOrigem,
                null,
                TipoPosicaoEmbarcacao.VAGA,
                vagaDestino,
                null,
                Instant.now().plusSeconds(3600),
                usuario,
                null,
                "Mudança definitiva de vaga");
        definirId(movimentacao, movimentacaoId);
        return movimentacao;
    }

    private void definirId(Movimentacao movimentacao, UUID id) {
        try {
            java.lang.reflect.Field campo = Movimentacao.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(movimentacao, id);
        } catch (ReflectiveOperationException excecao) {
            throw new IllegalStateException("Não foi possível preparar a movimentação para o teste", excecao);
        }
    }
}
