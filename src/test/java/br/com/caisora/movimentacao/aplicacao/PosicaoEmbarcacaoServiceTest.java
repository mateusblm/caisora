package br.com.caisora.movimentacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.movimentacao.api.PosicaoEmbarcacaoResponse;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacaoRepository;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.vaga.dominio.Vaga;

@ExtendWith(MockitoExtension.class)
class PosicaoEmbarcacaoServiceTest {

    @Mock
    private PosicaoEmbarcacaoRepository posicaoRepository;

    @Mock
    private EmbarcacaoRepository embarcacaoRepository;

    @Mock
    private OcupacaoRepository ocupacaoRepository;

    @Mock
    private PosicaoEmbarcacaoMapper posicaoMapper;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private PosicaoEmbarcacaoService service;

    private UUID organizacaoId;
    private UUID embarcacaoId;
    private Organizacao organizacao;
    private Embarcacao embarcacao;

    @BeforeEach
    void configurar() {
        service = new PosicaoEmbarcacaoService(
                posicaoRepository,
                embarcacaoRepository,
                ocupacaoRepository,
                posicaoMapper,
                leitorTokenJwt);

        organizacaoId = UUID.randomUUID();
        embarcacaoId = UUID.randomUUID();
        organizacao = org.mockito.Mockito.mock(Organizacao.class);
        embarcacao = org.mockito.Mockito.mock(Embarcacao.class);

        lenient().when(embarcacao.getId()).thenReturn(embarcacaoId);
        lenient().when(embarcacao.getOrganizacao()).thenReturn(organizacao);
        lenient().when(leitorTokenJwt.obterUsuarioAutenticado()).thenReturn(usuarioAutenticado());
    }

    @Test
    void deveRetornarPosicaoExistenteDaEmbarcacao() {
        PosicaoEmbarcacao posicao = PosicaoEmbarcacao.criarDesconhecida(organizacao, embarcacao);
        PosicaoEmbarcacaoResponse response = criarResponse(TipoPosicaoEmbarcacao.DESCONHECIDA, null);

        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.of(embarcacao));
        when(posicaoRepository.findByOrganizacaoIdAndEmbarcacaoId(organizacaoId, embarcacaoId))
                .thenReturn(Optional.of(posicao));
        when(posicaoMapper.paraResponse(posicao)).thenReturn(response);

        PosicaoEmbarcacaoResponse resultado = service.buscarPorEmbarcacao(embarcacaoId);

        assertThat(resultado).isSameAs(response);
        verify(posicaoRepository, never()).save(any(PosicaoEmbarcacao.class));
    }

    @Test
    void deveCriarPosicaoEmVagaQuandoExisteOcupacaoAtiva() {
        Vaga vaga = org.mockito.Mockito.mock(Vaga.class);
        Ocupacao ocupacao = org.mockito.Mockito.mock(Ocupacao.class);
        PosicaoEmbarcacaoResponse response = criarResponse(TipoPosicaoEmbarcacao.VAGA, UUID.randomUUID());

        when(ocupacao.getVaga()).thenReturn(vaga);
        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.of(embarcacao));
        when(posicaoRepository.findByOrganizacaoIdAndEmbarcacaoId(organizacaoId, embarcacaoId))
                .thenReturn(Optional.empty());
        when(ocupacaoRepository.findByOrganizacaoIdAndEmbarcacaoIdAndStatus(organizacaoId, embarcacaoId, StatusOcupacao.ATIVA))
                .thenReturn(Optional.of(ocupacao));
        when(posicaoRepository.save(any(PosicaoEmbarcacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(posicaoMapper.paraResponse(any(PosicaoEmbarcacao.class))).thenReturn(response);

        service.buscarPorEmbarcacao(embarcacaoId);

        ArgumentCaptor<PosicaoEmbarcacao> captor = ArgumentCaptor.forClass(PosicaoEmbarcacao.class);
        verify(posicaoRepository).save(captor.capture());

        PosicaoEmbarcacao criada = captor.getValue();
        assertThat(criada.getTipo()).isEqualTo(TipoPosicaoEmbarcacao.VAGA);
        assertThat(criada.getVaga()).isSameAs(vaga);
        assertThat(criada.getEmbarcacao()).isSameAs(embarcacao);
        assertThat(criada.getOrganizacao()).isSameAs(organizacao);
    }

    @Test
    void deveCriarPosicaoDesconhecidaQuandoNaoExisteOcupacaoAtiva() {
        PosicaoEmbarcacaoResponse response = criarResponse(TipoPosicaoEmbarcacao.DESCONHECIDA, null);

        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.of(embarcacao));
        when(posicaoRepository.findByOrganizacaoIdAndEmbarcacaoId(organizacaoId, embarcacaoId))
                .thenReturn(Optional.empty());
        when(ocupacaoRepository.findByOrganizacaoIdAndEmbarcacaoIdAndStatus(organizacaoId, embarcacaoId, StatusOcupacao.ATIVA))
                .thenReturn(Optional.empty());
        when(posicaoRepository.save(any(PosicaoEmbarcacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(posicaoMapper.paraResponse(any(PosicaoEmbarcacao.class))).thenReturn(response);

        service.buscarPorEmbarcacao(embarcacaoId);

        ArgumentCaptor<PosicaoEmbarcacao> captor = ArgumentCaptor.forClass(PosicaoEmbarcacao.class);
        verify(posicaoRepository).save(captor.capture());

        PosicaoEmbarcacao criada = captor.getValue();
        assertThat(criada.getTipo()).isEqualTo(TipoPosicaoEmbarcacao.DESCONHECIDA);
        assertThat(criada.getVaga()).isNull();
    }

    @Test
    void deveFalharQuandoEmbarcacaoNaoPertenceAOrganizacaoAutenticada() {
        when(embarcacaoRepository.findByIdAndOrganizacaoId(embarcacaoId, organizacaoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorEmbarcacao(embarcacaoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(posicaoRepository, never())
                .findByOrganizacaoIdAndEmbarcacaoId(any(UUID.class), any(UUID.class));
    }

    private UsuarioAutenticado usuarioAutenticado() {
        return new UsuarioAutenticado(
                UUID.randomUUID(),
                "Operador",
                "operador@caisora.com.br",
                PerfilUsuario.ADMINISTRADOR_MARINA,
                organizacaoId,
                "Marina Caisora");
    }

    private PosicaoEmbarcacaoResponse criarResponse(TipoPosicaoEmbarcacao tipo, UUID vagaId) {
        return new PosicaoEmbarcacaoResponse(
                UUID.randomUUID(),
                embarcacaoId,
                "Aurora",
                "Phantom 300",
                "Cliente Teste",
                tipo,
                vagaId,
                vagaId == null ? null : "A-01",
                vagaId == null ? null : "Setor A",
                vagaId == null ? null : "Galpão 1",
                null,
                null,
                0L,
                organizacaoId,
                Instant.now(),
                Instant.now());
    }
}
