package br.com.caisora.cliente.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.cliente.api.AlterarStatusClienteRequest;
import br.com.caisora.cliente.api.AtualizarClienteRequest;
import br.com.caisora.cliente.api.ClienteResponse;
import br.com.caisora.cliente.api.CriarClienteRequest;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.PerfilUsuario;

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
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private ClienteService clienteService;

    @BeforeEach
    void configurar() {
        clienteService = new ClienteService(
                clienteRepository,
                organizacaoRepository,
                new ClienteMapper(),
                leitorTokenJwt
        );
    }

    @Test
    void deveCriarClientePessoaFisicaComDadosNormalizados() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(organizacaoId);

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.FISICA,
                "  Joao da Silva  ",
                null,
                "529.982.247-25",
                "  JOAO@EMAIL.COM  ",
                "(41) 3333-4444",
                "(41) 99999-8888",
                "  Cliente mensalista  "
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(organizacao));

        when(clienteRepository.existsByOrganizacaoIdAndCpfCnpj(
                organizacaoId,
                "52998224725"
        )).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> {
                    Cliente cliente = invocacao.getArgument(0);
                    popularAuditoria(cliente, UUID.randomUUID());
                    return cliente;
                });

        ClienteResponse response = clienteService.criar(request);

        ArgumentCaptor<Cliente> captor =
                ArgumentCaptor.forClass(Cliente.class);

        verify(clienteRepository).save(captor.capture());

        Cliente clienteSalvo = captor.getValue();

        assertThat(clienteSalvo.getNome())
                .isEqualTo("Joao da Silva");

        assertThat(clienteSalvo.getCpfCnpj())
                .isEqualTo("52998224725");

        assertThat(clienteSalvo.getEmail())
                .isEqualTo("joao@email.com");

        assertThat(clienteSalvo.getTelefone())
                .isEqualTo("4133334444");

        assertThat(clienteSalvo.getCelular())
                .isEqualTo("41999998888");

        assertThat(clienteSalvo.getObservacoes())
                .isEqualTo("Cliente mensalista");

        assertThat(clienteSalvo.isAtivo()).isTrue();

        assertThat(response.tipoPessoa())
                .isEqualTo(TipoPessoa.FISICA);

        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);

        assertThat(response.ativo()).isTrue();
    }

    @Test
    void deveCriarClientePessoaJuridicaComRazaoSocial() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(organizacaoId);

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.JURIDICA,
                "Nautica Horizonte",
                "Nautica Horizonte LTDA",
                "12.345.678/0001-90",
                "CONTATO@NAUTICA.COM",
                "4133334444",
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(organizacao));

        when(clienteRepository.existsByOrganizacaoIdAndCpfCnpj(
                organizacaoId,
                "12345678000190"
        )).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> {
                    Cliente cliente = invocacao.getArgument(0);
                    popularAuditoria(cliente, UUID.randomUUID());
                    return cliente;
                });

        ClienteResponse response = clienteService.criar(request);

        assertThat(response.tipoPessoa())
                .isEqualTo(TipoPessoa.JURIDICA);

        assertThat(response.razaoSocial())
                .isEqualTo("Nautica Horizonte LTDA");

        assertThat(response.cpfCnpj())
                .isEqualTo("12345678000190");

        assertThat(response.email())
                .isEqualTo("contato@nautica.com");
    }

    @Test
    void deveFalharAoCriarPessoaFisicaComCpfInvalido() {
        UUID organizacaoId = UUID.randomUUID();

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.FISICA,
                "Joao da Silva",
                null,
                "123",
                "joao@email.com",
                null,
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(
                        criarOrganizacaoPersistida(organizacaoId)
                ));

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("CPF invalido");

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    @Test
    void deveFalharAoCriarPessoaJuridicaComCnpjInvalido() {
        UUID organizacaoId = UUID.randomUUID();

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.JURIDICA,
                "Empresa Teste",
                "Empresa Teste LTDA",
                "123",
                "empresa@email.com",
                null,
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(
                        criarOrganizacaoPersistida(organizacaoId)
                ));

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("CNPJ invalido");

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    @Test
    void deveFalharAoCriarPessoaJuridicaSemRazaoSocial() {
        UUID organizacaoId = UUID.randomUUID();

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.JURIDICA,
                "Empresa Teste",
                null,
                "12345678000190",
                "empresa@email.com",
                null,
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(
                        criarOrganizacaoPersistida(organizacaoId)
                ));

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(
                        "Razao social obrigatoria para pessoa juridica"
                );

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    @Test
    void deveFalharAoCriarClienteComDocumentoDuplicadoNaMesmaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.FISICA,
                "Joao da Silva",
                null,
                "529.982.247-25",
                "joao@email.com",
                null,
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(
                        criarOrganizacaoPersistida(organizacaoId)
                ));

        when(clienteRepository.existsByOrganizacaoIdAndCpfCnpj(
                organizacaoId,
                "52998224725"
        )).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage(
                        "Ja existe um cliente com este CPF ou CNPJ na organizacao"
                );

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    @Test
    void devePermitirMesmoDocumentoEmOrganizacoesDiferentes() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao =
                criarOrganizacaoPersistida(organizacaoId);

        CriarClienteRequest request = new CriarClienteRequest(
                TipoPessoa.FISICA,
                "Joao da Silva",
                null,
                "52998224725",
                "joao@email.com",
                null,
                null,
                null
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(organizacaoRepository.findById(organizacaoId))
                .thenReturn(Optional.of(organizacao));

        when(clienteRepository.existsByOrganizacaoIdAndCpfCnpj(
                organizacaoId,
                "52998224725"
        )).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> {
                    Cliente cliente = invocacao.getArgument(0);
                    popularAuditoria(cliente, UUID.randomUUID());
                    return cliente;
                });

        ClienteResponse response = clienteService.criar(request);

        assertThat(response.cpfCnpj())
                .isEqualTo("52998224725");

        verify(clienteRepository)
                .existsByOrganizacaoIdAndCpfCnpj(
                        organizacaoId,
                        "52998224725"
                );
    }

    @Test
    void deveListarSomenteClientesDaOrganizacaoAutenticada() {
        UUID organizacaoId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                UUID.randomUUID(),
                criarOrganizacaoPersistida(organizacaoId)
        );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findAllByOrganizacaoId(
                organizacaoId,
                paginacao
        )).thenReturn(new PageImpl<>(
                List.of(cliente),
                paginacao,
                1
        ));

        var pagina = clienteService.listar(paginacao);

        assertThat(pagina.getContent()).hasSize(1);

        assertThat(pagina.getContent().get(0).organizacaoId())
                .isEqualTo(organizacaoId);
    }

    @Test
    void deveListarClientesPorStatus() {
        UUID organizacaoId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                UUID.randomUUID(),
                criarOrganizacaoPersistida(organizacaoId)
        );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findAllByOrganizacaoIdAndAtivo(
                organizacaoId,
                true,
                paginacao
        )).thenReturn(new PageImpl<>(
                List.of(cliente),
                paginacao,
                1
        ));

        var pagina =
                clienteService.listarPorStatus(true, paginacao);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).ativo()).isTrue();
    }

    @Test
    void deveBuscarClientesPorNome() {
        UUID organizacaoId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                UUID.randomUUID(),
                criarOrganizacaoPersistida(organizacaoId)
        );

        PageRequest paginacao = PageRequest.of(0, 10);

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(
                clienteRepository
                        .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                                organizacaoId,
                                "Joao",
                                paginacao
                        )
        ).thenReturn(new PageImpl<>(
                List.of(cliente),
                paginacao,
                1
        ));

        var pagina =
                clienteService.buscarPorNome(
                        "  Joao  ",
                        paginacao
                );

        assertThat(pagina.getContent()).hasSize(1);

        assertThat(pagina.getContent().get(0).nome())
                .isEqualTo("Joao da Silva");

        verify(clienteRepository)
                .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                        organizacaoId,
                        "Joao",
                        paginacao
                );
    }

    @Test
    void deveBuscarClientePorIdDentroDaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                clienteId,
                criarOrganizacaoPersistida(organizacaoId)
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.of(cliente));

        ClienteResponse response =
                clienteService.buscarPorId(clienteId);

        assertThat(response.id()).isEqualTo(clienteId);

        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoBuscarClienteDeOutraOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> clienteService.buscarPorId(clienteId)
        )
                .isInstanceOf(
                        RecursoNaoEncontradoException.class
                )
                .hasMessage("Cliente nao encontrado");
    }

    @Test
    void deveAtualizarClienteSemAlterarOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                clienteId,
                criarOrganizacaoPersistida(organizacaoId)
        );

        AtualizarClienteRequest request =
                new AtualizarClienteRequest(
                        TipoPessoa.FISICA,
                        "Joao Atualizado",
                        null,
                        "529.982.247-25",
                        "NOVO@EMAIL.COM",
                        "(41) 3333-5555",
                        "(41) 99999-7777",
                        "Dados atualizados"
                );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.of(cliente));

        when(
                clienteRepository
                        .existsByOrganizacaoIdAndCpfCnpjAndIdNot(
                                organizacaoId,
                                "52998224725",
                                clienteId
                        )
        ).thenReturn(false);

        when(clienteRepository.save(cliente))
                .thenReturn(cliente);

        ClienteResponse response =
                clienteService.atualizar(clienteId, request);

        assertThat(response.nome())
                .isEqualTo("Joao Atualizado");

        assertThat(response.email())
                .isEqualTo("novo@email.com");

        assertThat(response.telefone())
                .isEqualTo("4133335555");

        assertThat(response.organizacaoId())
                .isEqualTo(organizacaoId);

        assertThat(cliente.getOrganizacao().getId())
                .isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoAtualizarClienteComDocumentoDuplicado() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                clienteId,
                criarOrganizacaoPersistida(organizacaoId)
        );

        AtualizarClienteRequest request =
                new AtualizarClienteRequest(
                        TipoPessoa.FISICA,
                        "Joao da Silva",
                        null,
                        "111.222.333-44",
                        "joao@email.com",
                        null,
                        null,
                        null
                );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.of(cliente));

        when(
                clienteRepository
                        .existsByOrganizacaoIdAndCpfCnpjAndIdNot(
                                organizacaoId,
                                "11122233344",
                                clienteId
                        )
        ).thenReturn(true);

        assertThatThrownBy(
                () -> clienteService.atualizar(
                        clienteId,
                        request
                )
        )
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage(
                        "Ja existe um cliente com este CPF ou CNPJ na organizacao"
                );

        verify(clienteRepository, never())
                .save(cliente);
    }

    @Test
    void deveInativarCliente() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                clienteId,
                criarOrganizacaoPersistida(organizacaoId)
        );

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.of(cliente));

        when(clienteRepository.save(cliente))
                .thenReturn(cliente);

        ClienteResponse response =
                clienteService.alterarStatus(
                        clienteId,
                        new AlterarStatusClienteRequest(false)
                );

        assertThat(response.ativo()).isFalse();
        assertThat(cliente.isAtivo()).isFalse();

        verify(clienteRepository).save(cliente);
    }

    @Test
    void deveAtivarCliente() {
        UUID organizacaoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();

        Cliente cliente = criarClientePersistido(
                clienteId,
                criarOrganizacaoPersistida(organizacaoId)
        );

        cliente.inativar();

        when(leitorTokenJwt.obterUsuarioAutenticado())
                .thenReturn(criarUsuarioAutenticado(organizacaoId));

        when(clienteRepository.findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
        )).thenReturn(Optional.of(cliente));

        when(clienteRepository.save(cliente))
                .thenReturn(cliente);

        ClienteResponse response =
                clienteService.alterarStatus(
                        clienteId,
                        new AlterarStatusClienteRequest(true)
                );

        assertThat(response.ativo()).isTrue();
        assertThat(cliente.isAtivo()).isTrue();
    }

    private Organizacao criarOrganizacaoPersistida(UUID id) {
        Organizacao organizacao = Organizacao.criar(
                "Marina Teste",
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

        popularAuditoria(cliente, id);

        return cliente;
    }

    private void popularAuditoria(
            Cliente cliente,
            UUID id
    ) {
        ReflectionTestUtils.setField(
                cliente,
                "id",
                id
        );

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