package br.com.caisora.embarcacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.api.AlterarStatusEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.AtualizarEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.CriarEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.EmbarcacaoResponse;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.PerfilUsuario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
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
class EmbarcacaoServiceTest {

    @Mock
    private EmbarcacaoRepository embarcacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private EmbarcacaoService embarcacaoService;

    @BeforeEach
    void configurar() {
        embarcacaoService = new EmbarcacaoService(
                embarcacaoRepository,
                clienteRepository,
                organizacaoRepository,
                new EmbarcacaoMapper(),
                leitorTokenJwt
        );
    }

    @Test
    void deveCriarEmbarcacaoComDadosNormalizados() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        CriarEmbarcacaoRequest request =
                criarRequest(proprietarioId);

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCase(
                                organizacaoId,
                                "PR-123456"
                        )
        ).thenReturn(false);

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroCascoIgnoreCase(
                                organizacaoId,
                                "BR-SCH12345A323"
                        )
        ).thenReturn(false);

        when(embarcacaoRepository.save(any(Embarcacao.class)))
                .thenAnswer(invocacao -> {
                    Embarcacao embarcacao =
                            invocacao.getArgument(0);

                    popularAuditoria(
                            embarcacao,
                            UUID.randomUUID()
                    );

                    return embarcacao;
                });

        EmbarcacaoResponse response =
                embarcacaoService.criar(request);

        ArgumentCaptor<Embarcacao> captor =
                ArgumentCaptor.forClass(Embarcacao.class);

        verify(embarcacaoRepository).save(captor.capture());

        Embarcacao salva = captor.getValue();

        assertThat(salva.getNome()).isEqualTo("Aurora");
        assertThat(salva.getFabricante())
                .isEqualTo("Schaefer");
        assertThat(salva.getModelo()).isEqualTo("V33");
        assertThat(salva.getNumeroInscricao())
                .isEqualTo("PR-123456");
        assertThat(salva.getNumeroCasco())
                .isEqualTo("BR-SCH12345A323");
        assertThat(salva.getCodigoPaisBandeira())
                .isEqualTo("BR");
        assertThat(salva.getCorPredominante())
                .isEqualTo("Branca");
        assertThat(salva.getObservacoes())
                .isEqualTo("Embarcacao principal");
        assertThat(salva.isAtiva()).isTrue();

        assertThat(response.proprietarioId())
                .isEqualTo(proprietarioId);
        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);
        assertThat(response.tipo())
                .isEqualTo(TipoEmbarcacao.LANCHA);
        assertThat(response.ativa()).isTrue();
    }

    @Test
    void deveUsarBrasilQuandoCodigoPaisNaoForInformado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        CriarEmbarcacaoRequest request =
                new CriarEmbarcacaoRequest(
                        proprietarioId,
                        "Aurora",
                        TipoEmbarcacao.LANCHA,
                        null,
                        null,
                        2023,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("10.00"),
                        new BigDecimal("3.00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        TipoPropulsao.MOTOR,
                        null,
                        null
                );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(embarcacaoRepository.save(any(Embarcacao.class)))
                .thenAnswer(invocacao -> {
                    Embarcacao embarcacao =
                            invocacao.getArgument(0);

                    popularAuditoria(
                            embarcacao,
                            UUID.randomUUID()
                    );

                    return embarcacao;
                });

        EmbarcacaoResponse response =
                embarcacaoService.criar(request);

        assertThat(response.codigoPaisBandeira())
                .isEqualTo("BR");
    }

    @Test
    void deveFalharAoCriarComProprietarioInexistenteNaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                embarcacaoService.criar(
                        criarRequest(proprietarioId)
                )
        )
                .isInstanceOf(
                        RecursoNaoEncontradoException.class
                )
                .hasMessage("Proprietario nao encontrado");

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComProprietarioInativo() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                false
        );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        assertThatThrownBy(() ->
                embarcacaoService.criar(
                        criarRequest(proprietarioId)
                )
        )
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(
                        "Nao e permitido vincular uma embarcacao "
                                + "a um proprietario inativo"
                );

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComNumeroInscricaoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCase(
                                organizacaoId,
                                "PR-123456"
                        )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                embarcacaoService.criar(
                        criarRequest(proprietarioId)
                )
        )
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage(
                        "Ja existe uma embarcacao com este "
                                + "numero de inscricao na organizacao"
                );

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComNumeroCascoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroCascoIgnoreCase(
                                organizacaoId,
                                "BR-SCH12345A323"
                        )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                embarcacaoService.criar(
                        criarRequest(proprietarioId)
                )
        )
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage(
                        "Ja existe uma embarcacao com este "
                                + "numero de casco na organizacao"
                );

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComComprimentoInvalido() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        CriarEmbarcacaoRequest request =
                new CriarEmbarcacaoRequest(
                        proprietarioId,
                        "Aurora",
                        TipoEmbarcacao.LANCHA,
                        null,
                        null,
                        2023,
                        null,
                        null,
                        null,
                        "BR",
                        BigDecimal.ZERO,
                        new BigDecimal("3.00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        TipoPropulsao.MOTOR,
                        null,
                        null
                );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        assertThatThrownBy(() ->
                embarcacaoService.criar(request)
        )
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(
                        "Comprimento total deve ser maior que zero"
                );

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComBocaInvalida() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        CriarEmbarcacaoRequest request =
                new CriarEmbarcacaoRequest(
                        proprietarioId,
                        "Aurora",
                        TipoEmbarcacao.LANCHA,
                        null,
                        null,
                        2023,
                        null,
                        null,
                        null,
                        "BR",
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TipoPropulsao.MOTOR,
                        null,
                        null
                );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        assertThatThrownBy(() ->
                embarcacaoService.criar(request)
        )
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Boca deve ser maior que zero");

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveFalharAoCriarComAnoFabricacaoInvalido() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        CriarEmbarcacaoRequest request =
                new CriarEmbarcacaoRequest(
                        proprietarioId,
                        "Aurora",
                        TipoEmbarcacao.LANCHA,
                        null,
                        null,
                        Year.now().getValue() + 2,
                        null,
                        null,
                        null,
                        "BR",
                        new BigDecimal("10.00"),
                        new BigDecimal("3.00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        TipoPropulsao.MOTOR,
                        null,
                        null
                );

        prepararContextoAutenticado(
                organizacaoId,
                organizacao
        );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        assertThatThrownBy(() ->
                embarcacaoService.criar(request)
        )
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Ano de fabricacao invalido");

        verify(embarcacaoRepository, never())
                .save(any(Embarcacao.class));
    }

    @Test
    void deveListarSomenteEmbarcacoesDaOrganizacaoAutenticada() {
        UUID organizacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        UUID.randomUUID(),
                        organizacao,
                        proprietario
                );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findAllByOrganizacaoId(
                organizacaoId,
                paginacao
        )).thenReturn(new PageImpl<>(
                List.of(embarcacao),
                paginacao,
                1
        ));

        var pagina = embarcacaoService.listar(paginacao);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(
                pagina.getContent().get(0).organizacaoId()
        ).isEqualTo(organizacaoId);
    }

    @Test
    void deveListarEmbarcacoesPorStatus() {
        UUID organizacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        UUID.randomUUID(),
                        organizacao,
                        proprietario
                );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndAtiva(
                                organizacaoId,
                                true,
                                paginacao
                        )
        ).thenReturn(new PageImpl<>(
                List.of(embarcacao),
                paginacao,
                1
        ));

        var pagina = embarcacaoService.listarPorStatus(
                true,
                paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).ativa())
                .isTrue();
    }

    @Test
    void deveBuscarEmbarcacoesPorNome() {
        UUID organizacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        UUID.randomUUID(),
                        organizacao,
                        proprietario
                );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                                organizacaoId,
                                "Aurora",
                                paginacao
                        )
        ).thenReturn(new PageImpl<>(
                List.of(embarcacao),
                paginacao,
                1
        ));

        var pagina = embarcacaoService.buscarPorNome(
                " Aurora ",
                paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).nome())
                .isEqualTo("Aurora");

        verify(embarcacaoRepository)
                .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                        organizacaoId,
                        "Aurora",
                        paginacao
                );
    }

    @Test
    void deveListarEmbarcacoesPorProprietario() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        UUID.randomUUID(),
                        organizacao,
                        proprietario
                );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndProprietarioId(
                                organizacaoId,
                                proprietarioId,
                                paginacao
                        )
        ).thenReturn(new PageImpl<>(
                List.of(embarcacao),
                paginacao,
                1
        ));

        var pagina =
                embarcacaoService.listarPorProprietario(
                        proprietarioId,
                        paginacao
                );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(
                pagina.getContent().get(0).proprietarioId()
        ).isEqualTo(proprietarioId);
    }

    @Test
    void deveListarEmbarcacoesPorTipo() {
        UUID organizacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        UUID.randomUUID(),
                        organizacao,
                        proprietario
                );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findAllByOrganizacaoIdAndTipo(
                organizacaoId,
                TipoEmbarcacao.LANCHA,
                paginacao
        )).thenReturn(new PageImpl<>(
                List.of(embarcacao),
                paginacao,
                1
        ));

        var pagina = embarcacaoService.listarPorTipo(
                TipoEmbarcacao.LANCHA,
                paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).tipo())
                .isEqualTo(TipoEmbarcacao.LANCHA);
    }

    @Test
    void deveBuscarEmbarcacaoPorIdDentroDaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        EmbarcacaoResponse response =
                embarcacaoService.buscarPorId(embarcacaoId);

        assertThat(response.id()).isEqualTo(embarcacaoId);
        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoBuscarEmbarcacaoDeOutraOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                embarcacaoService.buscarPorId(embarcacaoId)
        )
                .isInstanceOf(
                        RecursoNaoEncontradoException.class
                )
                .hasMessage("Embarcacao nao encontrada");
    }

    @Test
    void deveAtualizarEmbarcacaoSemAlterarOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        AtualizarEmbarcacaoRequest request =
                criarRequestAtualizacao(proprietarioId);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
                                organizacaoId,
                                "PR-654321",
                                embarcacaoId
                        )
        ).thenReturn(false);

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroCascoIgnoreCaseAndIdNot(
                                organizacaoId,
                                "BR-NOVO123",
                                embarcacaoId
                        )
        ).thenReturn(false);

        when(embarcacaoRepository.save(embarcacao))
                .thenReturn(embarcacao);

        EmbarcacaoResponse response =
                embarcacaoService.atualizar(
                        embarcacaoId,
                        request
                );

        assertThat(response.nome())
                .isEqualTo("Aurora Atualizada");
        assertThat(response.numeroInscricao())
                .isEqualTo("PR-654321");
        assertThat(response.numeroCasco())
                .isEqualTo("BR-NOVO123");
        assertThat(response.codigoPaisBandeira())
                .isEqualTo("BR");
        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);

        assertThat(embarcacao.getOrganizacao().getId())
                .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoAtualizarComNumeroInscricaoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        AtualizarEmbarcacaoRequest request =
                criarRequestAtualizacao(proprietarioId);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
                                organizacaoId,
                                "PR-654321",
                                embarcacaoId
                        )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                embarcacaoService.atualizar(
                        embarcacaoId,
                        request
                )
        )
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage(
                        "Ja existe uma embarcacao com este "
                                + "numero de inscricao na organizacao"
                );

        verify(embarcacaoRepository, never())
                .save(embarcacao);
    }

    @Test
    void deveFalharAoTrocarParaProprietarioInativo() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioAtualId = UUID.randomUUID();
        UUID novoProprietarioId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietarioAtual =
                criarClientePersistido(
                        proprietarioAtualId,
                        organizacao,
                        true
                );

        Cliente novoProprietario =
                criarClientePersistido(
                        novoProprietarioId,
                        organizacao,
                        false
                );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietarioAtual
                );

        AtualizarEmbarcacaoRequest request =
                criarRequestAtualizacao(novoProprietarioId);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(clienteRepository.findByIdAndOrganizacaoId(
                novoProprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(novoProprietario));

        assertThatThrownBy(() ->
                embarcacaoService.atualizar(
                        embarcacaoId,
                        request
                )
        )
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(
                        "Nao e permitido vincular uma embarcacao "
                                + "a um proprietario inativo"
                );

        verify(embarcacaoRepository, never())
                .save(embarcacao);
    }

    @Test
    void devePermitirManterProprietarioAtualMesmoInativo() {
        UUID organizacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                proprietarioId,
                organizacao,
                false
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        AtualizarEmbarcacaoRequest request =
                criarRequestAtualizacao(proprietarioId);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(clienteRepository.findByIdAndOrganizacaoId(
                proprietarioId,
                organizacaoId
        )).thenReturn(Optional.of(proprietario));

        when(embarcacaoRepository.save(embarcacao))
                .thenReturn(embarcacao);

        EmbarcacaoResponse response =
                embarcacaoService.atualizar(
                        embarcacaoId,
                        request
                );

        assertThat(response.proprietarioId())
                .isEqualTo(proprietarioId);
    }

    @Test
    void deveInativarEmbarcacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(embarcacaoRepository.save(embarcacao))
                .thenReturn(embarcacao);

        EmbarcacaoResponse response =
                embarcacaoService.alterarStatus(
                        embarcacaoId,
                        new AlterarStatusEmbarcacaoRequest(false)
                );

        assertThat(response.ativa()).isFalse();
        assertThat(embarcacao.isAtiva()).isFalse();

        verify(embarcacaoRepository).save(embarcacao);
    }

    @Test
    void deveAtivarEmbarcacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();

        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        Cliente proprietario = criarClientePersistido(
                UUID.randomUUID(),
                organizacao,
                true
        );

        Embarcacao embarcacao =
                criarEmbarcacaoPersistida(
                        embarcacaoId,
                        organizacao,
                        proprietario
                );

        embarcacao.inativar();

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(embarcacaoRepository.findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
        )).thenReturn(Optional.of(embarcacao));

        when(embarcacaoRepository.save(embarcacao))
                .thenReturn(embarcacao);

        EmbarcacaoResponse response =
                embarcacaoService.alterarStatus(
                        embarcacaoId,
                        new AlterarStatusEmbarcacaoRequest(true)
                );

        assertThat(response.ativa()).isTrue();
        assertThat(embarcacao.isAtiva()).isTrue();
    }

    private CriarEmbarcacaoRequest criarRequest(
            UUID proprietarioId
    ) {
        return new CriarEmbarcacaoRequest(
                proprietarioId,
                " Aurora ",
                TipoEmbarcacao.LANCHA,
                " Schaefer ",
                " V33 ",
                2023,
                " pr-123456 ",
                " br-sch12345a323 ",
                " Paranagua ",
                " br ",
                new BigDecimal("10.33"),
                new BigDecimal("3.35"),
                new BigDecimal("0.95"),
                new BigDecimal("1.70"),
                new BigDecimal("3.60"),
                new BigDecimal("5200.00"),
                12,
                TipoPropulsao.MOTOR,
                " Branca ",
                " Embarcacao principal "
        );
    }

    private AtualizarEmbarcacaoRequest
    criarRequestAtualizacao(UUID proprietarioId) {
        return new AtualizarEmbarcacaoRequest(
                proprietarioId,
                " Aurora Atualizada ",
                TipoEmbarcacao.LANCHA,
                " Schaefer ",
                " V33 ",
                2024,
                " pr-654321 ",
                " br-novo123 ",
                " Paranagua ",
                " br ",
                new BigDecimal("10.50"),
                new BigDecimal("3.40"),
                new BigDecimal("1.00"),
                new BigDecimal("1.75"),
                new BigDecimal("3.70"),
                new BigDecimal("5400.00"),
                12,
                TipoPropulsao.MOTOR,
                " Branca e azul ",
                " Dados atualizados "
        );
    }

    private void prepararContextoAutenticado(
            UUID organizacaoId,
            Organizacao organizacao
    ) {
        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(
                        criarUsuarioAutenticado(organizacaoId)
                );

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(organizacao));
    }

    private Organizacao criarOrganizacaoPersistida(UUID id) {
        Organizacao organizacao = Organizacao.criar(
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
                Instant.parse("2026-06-28T20:00:00Z")
        );

        ReflectionTestUtils.setField(
                organizacao,
                "atualizadaEm",
                Instant.parse("2026-06-28T20:00:00Z")
        );

        return organizacao;
    }

    private Cliente criarClientePersistido(
            UUID id,
            Organizacao organizacao,
            boolean ativo
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

        ReflectionTestUtils.setField(cliente, "id", id);
        ReflectionTestUtils.setField(
                cliente,
                "criadoEm",
                Instant.parse("2026-06-28T20:00:00Z")
        );
        ReflectionTestUtils.setField(
                cliente,
                "atualizadoEm",
                Instant.parse("2026-06-28T20:00:00Z")
        );

        if (!ativo) {
            cliente.inativar();
        }

        return cliente;
    }

    private Embarcacao criarEmbarcacaoPersistida(
            UUID id,
            Organizacao organizacao,
            Cliente proprietario
    ) {
        Embarcacao embarcacao = new Embarcacao(
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
                new BigDecimal("10.33"),
                new BigDecimal("3.35"),
                new BigDecimal("0.95"),
                new BigDecimal("1.70"),
                new BigDecimal("3.60"),
                new BigDecimal("5200.00"),
                12,
                TipoPropulsao.MOTOR,
                "Branca",
                "Embarcacao principal"
        );

        popularAuditoria(embarcacao, id);

        return embarcacao;
    }

    private void popularAuditoria(
            Embarcacao embarcacao,
            UUID id
    ) {
        ReflectionTestUtils.setField(
                embarcacao,
                "id",
                id
        );

        ReflectionTestUtils.setField(
                embarcacao,
                "criadaEm",
                Instant.parse("2026-06-28T20:00:00Z")
        );

        ReflectionTestUtils.setField(
                embarcacao,
                "atualizadaEm",
                Instant.parse("2026-06-28T20:00:00Z")
        );
    }

    private UsuarioAutenticado criarUsuarioAutenticado(
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
}
