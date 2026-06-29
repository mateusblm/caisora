package br.com.caisora.vaga.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.vaga.api.AlterarStatusVagaRequest;
import br.com.caisora.vaga.api.AtualizarVagaRequest;
import br.com.caisora.vaga.api.CriarVagaRequest;
import br.com.caisora.vaga.api.VagaResponse;
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
class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private VagaService vagaService;

    @BeforeEach
    void configurar() {
        vagaService = new VagaService(
            vagaRepository,
            organizacaoRepository,
            new VagaMapper(),
            leitorTokenJwt
        );
    }

    @Test
    void deveCriarVagaComDadosNormalizados() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        prepararContextoAutenticado(
            organizacaoId,
            organizacao
        );

        when(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoId,
                    "A-01"
                )
        ).thenReturn(false);

        when(vagaRepository.save(any(Vaga.class)))
            .thenAnswer(invocacao -> {
                Vaga vaga = invocacao.getArgument(0);
                popularAuditoria(
                    vaga,
                    UUID.randomUUID()
                );
                return vaga;
            });

        VagaResponse response =
            vagaService.criar(criarRequest());

        ArgumentCaptor<Vaga> captor =
            ArgumentCaptor.forClass(Vaga.class);

        verify(vagaRepository).save(captor.capture());

        Vaga salva = captor.getValue();

        assertThat(salva.getCodigo())
            .isEqualTo("A-01");

        assertThat(salva.getSetor())
            .isEqualTo("Pier A");

        assertThat(salva.getLocalizacao())
            .isEqualTo("Corredor principal");

        assertThat(salva.getObservacoes())
            .isEqualTo("Próxima à recepção");

        assertThat(salva.getTipo())
            .isEqualTo(TipoVaga.MOLHADA);

        assertThat(salva.isPossuiAgua()).isTrue();
        assertThat(salva.isPossuiEnergia()).isTrue();
        assertThat(salva.isAtiva()).isTrue();

        assertThat(response.codigo())
            .isEqualTo("A-01");

        assertThat(response.organizacaoId())
            .isEqualTo(organizacaoId);

        assertThat(response.ativa()).isTrue();
    }

    @Test
    void deveFalharAoCriarComCodigoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        prepararContextoAutenticado(
            organizacaoId,
            organizacao
        );

        when(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoId,
                    "A-01"
                )
        ).thenReturn(true);

        assertThatThrownBy(
            () -> vagaService.criar(criarRequest())
        )
            .isInstanceOf(ConflitoDadosException.class)
            .hasMessage(
                "Ja existe uma vaga com este "
                    + "codigo na organizacao"
            );

        verify(vagaRepository, never())
            .save(any(Vaga.class));
    }

    @Test
    void deveFalharAoCriarComComprimentoInvalido() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        prepararContextoAutenticado(
            organizacaoId,
            organizacao
        );

        CriarVagaRequest request =
            new CriarVagaRequest(
                "A-01",
                TipoVaga.MOLHADA,
                "Pier A",
                "Corredor principal",
                BigDecimal.ZERO,
                new BigDecimal("4.00"),
                new BigDecimal("1.50"),
                null,
                new BigDecimal("9000.00"),
                true,
                true,
                null
            );

        assertThatThrownBy(
            () -> vagaService.criar(request)
        )
            .isInstanceOf(DadosInvalidosException.class)
            .hasMessage(
                "Comprimento maximo deve ser "
                    + "maior que zero"
            );

        verify(vagaRepository, never())
            .save(any(Vaga.class));
    }

    @Test
    void deveFalharAoCriarComBocaInvalida() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        prepararContextoAutenticado(
            organizacaoId,
            organizacao
        );

        CriarVagaRequest request =
            new CriarVagaRequest(
                "A-01",
                TipoVaga.MOLHADA,
                "Pier A",
                null,
                new BigDecimal("12.00"),
                BigDecimal.ZERO,
                null,
                null,
                null,
                false,
                false,
                null
            );

        assertThatThrownBy(
            () -> vagaService.criar(request)
        )
            .isInstanceOf(DadosInvalidosException.class)
            .hasMessage(
                "Boca maxima deve ser maior "
                    + "que zero"
            );

        verify(vagaRepository, never())
            .save(any(Vaga.class));
    }

    @Test
    void deveListarSomenteVagasDaOrganizacaoAutenticada() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            "A-01"
        );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findAllByOrganizacaoId(
                organizacaoId,
                paginacao
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(vaga),
                paginacao,
                1
            )
        );

        var pagina = vagaService.listar(paginacao);

        assertThat(pagina.getContent()).hasSize(1);

        assertThat(
            pagina.getContent().get(0).organizacaoId()
        ).isEqualTo(organizacaoId);
    }

    @Test
    void deveListarVagasPorStatus() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            "A-01"
        );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository
                .findAllByOrganizacaoIdAndAtiva(
                    organizacaoId,
                    true,
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(vaga),
                paginacao,
                1
            )
        );

        var pagina = vagaService.listarPorStatus(
            true,
            paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);

        assertThat(
            pagina.getContent().get(0).ativa()
        ).isTrue();
    }

    @Test
    void deveBuscarVagasPorCodigoNormalizado() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            "A-01"
        );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository
                .findAllByOrganizacaoIdAndCodigoContainingIgnoreCase(
                    organizacaoId,
                    "A-01",
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(vaga),
                paginacao,
                1
            )
        );

        var pagina = vagaService.buscarPorCodigo(
            " A-01 ",
            paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);

        verify(vagaRepository)
            .findAllByOrganizacaoIdAndCodigoContainingIgnoreCase(
                organizacaoId,
                "A-01",
                paginacao
            );
    }

    @Test
    void deveBuscarVagasPorSetor() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            "A-01"
        );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository
                .findAllByOrganizacaoIdAndSetorContainingIgnoreCase(
                    organizacaoId,
                    "Pier A",
                    paginacao
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(vaga),
                paginacao,
                1
            )
        );

        var pagina = vagaService.buscarPorSetor(
            " Pier A ",
            paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);

        verify(vagaRepository)
            .findAllByOrganizacaoIdAndSetorContainingIgnoreCase(
                organizacaoId,
                "Pier A",
                paginacao
            );
    }

    @Test
    void deveListarVagasPorTipo() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            UUID.randomUUID(),
            organizacao,
            "A-01"
        );

        PageRequest paginacao =
            PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findAllByOrganizacaoIdAndTipo(
                organizacaoId,
                TipoVaga.MOLHADA,
                paginacao
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(vaga),
                paginacao,
                1
            )
        );

        var pagina = vagaService.listarPorTipo(
            TipoVaga.MOLHADA,
            paginacao
        );

        assertThat(pagina.getContent()).hasSize(1);

        assertThat(
            pagina.getContent().get(0).tipo()
        ).isEqualTo(TipoVaga.MOLHADA);
    }

    @Test
    void deveBuscarVagaPorIdDentroDaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            "A-01"
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        VagaResponse response =
            vagaService.buscarPorId(vagaId);

        assertThat(response.id()).isEqualTo(vagaId);

        assertThat(response.organizacaoId())
            .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoBuscarVagaDeOutraOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> vagaService.buscarPorId(vagaId)
        )
            .isInstanceOf(
                RecursoNaoEncontradoException.class
            )
            .hasMessage("Vaga nao encontrada");
    }

    @Test
    void deveAtualizarVagaSemAlterarOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            "A-01"
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        when(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
                    organizacaoId,
                    "B-02",
                    vagaId
                )
        ).thenReturn(false);

        when(vagaRepository.save(vaga))
            .thenReturn(vaga);

        VagaResponse response =
            vagaService.atualizar(
                vagaId,
                criarRequestAtualizacao()
            );

        assertThat(response.codigo())
            .isEqualTo("B-02");

        assertThat(response.tipo())
            .isEqualTo(TipoVaga.SECA);

        assertThat(response.setor())
            .isEqualTo("Pátio B");

        assertThat(response.organizacaoId())
            .isEqualTo(organizacaoId);

        assertThat(vaga.getOrganizacao().getId())
            .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoAtualizarComCodigoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            "A-01"
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        when(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
                    organizacaoId,
                    "B-02",
                    vagaId
                )
        ).thenReturn(true);

        assertThatThrownBy(
            () -> vagaService.atualizar(
                vagaId,
                criarRequestAtualizacao()
            )
        )
            .isInstanceOf(ConflitoDadosException.class)
            .hasMessage(
                "Ja existe uma vaga com este "
                    + "codigo na organizacao"
            );

        verify(vagaRepository, never()).save(vaga);
    }

    @Test
    void deveInativarVaga() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            "A-01"
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        when(vagaRepository.save(vaga))
            .thenReturn(vaga);

        VagaResponse response =
            vagaService.alterarStatus(
                vagaId,
                new AlterarStatusVagaRequest(false)
            );

        assertThat(response.ativa()).isFalse();
        assertThat(vaga.isAtiva()).isFalse();

        verify(vagaRepository).save(vaga);
    }

    @Test
    void deveAtivarVaga() {
        UUID organizacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        Organizacao organizacao =
            criarOrganizacaoPersistida(organizacaoId);

        Vaga vaga = criarVagaPersistida(
            vagaId,
            organizacao,
            "A-01"
        );

        vaga.inativar();

        when(leitorTokenJwt.obterUsuarioAutenticado())
            .thenReturn(
                criarUsuarioAutenticado(organizacaoId)
            );

        when(
            vagaRepository.findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
        ).thenReturn(Optional.of(vaga));

        when(vagaRepository.save(vaga))
            .thenReturn(vaga);

        VagaResponse response =
            vagaService.alterarStatus(
                vagaId,
                new AlterarStatusVagaRequest(true)
            );

        assertThat(response.ativa()).isTrue();
        assertThat(vaga.isAtiva()).isTrue();
    }

    @Test
    void deveFalharAoAlterarStatusNulo() {
        UUID vagaId = UUID.randomUUID();

        assertThatThrownBy(
            () -> vagaService.alterarStatus(
                vagaId,
                new AlterarStatusVagaRequest(null)
            )
        )
            .isInstanceOf(DadosInvalidosException.class)
            .hasMessage("Status da vaga obrigatorio");

        verify(vagaRepository, never())
            .findByIdAndOrganizacaoId(
                any(UUID.class),
                any(UUID.class)
            );
    }

    private CriarVagaRequest criarRequest() {
        return new CriarVagaRequest(
            " a-01 ",
            TipoVaga.MOLHADA,
            " Pier A ",
            " Corredor principal ",
            new BigDecimal("12.50"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("5.00"),
            new BigDecimal("9000.00"),
            true,
            true,
            " Próxima à recepção "
        );
    }

    private AtualizarVagaRequest
    criarRequestAtualizacao() {
        return new AtualizarVagaRequest(
            " b-02 ",
            TipoVaga.SECA,
            " Pátio B ",
            " Área coberta ",
            new BigDecimal("14.00"),
            new BigDecimal("4.50"),
            new BigDecimal("1.80"),
            new BigDecimal("6.00"),
            new BigDecimal("12000.00"),
            false,
            true,
            " Vaga atualizada "
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

    private Organizacao criarOrganizacaoPersistida(
        UUID id
    ) {
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
            Instant.parse("2026-06-29T20:00:00Z")
        );

        ReflectionTestUtils.setField(
            organizacao,
            "atualizadaEm",
            Instant.parse("2026-06-29T20:00:00Z")
        );

        return organizacao;
    }

    private Vaga criarVagaPersistida(
        UUID id,
        Organizacao organizacao,
        String codigo
    ) {
        Vaga vaga = new Vaga(
            organizacao,
            codigo,
            TipoVaga.MOLHADA,
            "Pier A",
            "Corredor principal",
            new BigDecimal("12.50"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("5.00"),
            new BigDecimal("9000.00"),
            true,
            true,
            "Próxima à recepção"
        );

        popularAuditoria(vaga, id);
        return vaga;
    }

    private void popularAuditoria(
        Vaga vaga,
        UUID id
    ) {
        ReflectionTestUtils.setField(
            vaga,
            "id",
            id
        );

        ReflectionTestUtils.setField(
            vaga,
            "criadaEm",
            Instant.parse("2026-06-29T20:00:00Z")
        );

        ReflectionTestUtils.setField(
            vaga,
            "atualizadaEm",
            Instant.parse("2026-06-29T20:00:00Z")
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
