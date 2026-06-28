package br.com.caisora.cliente.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.cliente.aplicacao.ClienteService;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        ClienteController controller =
                new ClienteController(clienteService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new TratadorGlobalException())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void deveCriarCliente() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = criarResponse(
                clienteId,
                organizacaoId,
                true
        );

        when(clienteService.criar(any(CriarClienteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipoPessoa": "FISICA",
                              "nome": "Joao da Silva",
                              "razaoSocial": null,
                              "cpfCnpj": "529.982.247-25",
                              "email": "joao@email.com",
                              "telefone": "4133334444",
                              "celular": "41999998888",
                              "observacoes": "Cliente mensalista"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/clientes/" + clienteId
                ))
                .andExpect(jsonPath("$.id")
                        .value(clienteId.toString()))
                .andExpect(jsonPath("$.nome")
                        .value("Joao da Silva"))
                .andExpect(jsonPath("$.tipoPessoa")
                        .value("FISICA"))
                .andExpect(jsonPath("$.ativo")
                        .value(true))
                .andExpect(jsonPath("$.organizacaoId")
                        .value(organizacaoId.toString()));
    }

    @Test
    void deveRetornarBadRequestAoCriarClienteInvalido()
            throws Exception {

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipoPessoa": null,
                              "nome": "",
                              "cpfCnpj": "",
                              "email": "email-invalido"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ERRO_VALIDACAO"))
                .andExpect(jsonPath("$.errosCampos")
                        .isArray());
    }

    @Test
    void deveRetornarBadRequestParaCpfInvalido()
            throws Exception {

        when(clienteService.criar(any(CriarClienteRequest.class)))
                .thenThrow(new DadosInvalidosException(
                        "CPF_INVALIDO",
                        "CPF invalido"
                ));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipoPessoa": "FISICA",
                              "nome": "Joao da Silva",
                              "cpfCnpj": "123",
                              "email": "joao@email.com"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("CPF_INVALIDO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("CPF invalido"));
    }

    @Test
    void deveListarClientes() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = criarResponse(
                UUID.randomUUID(),
                organizacaoId,
                true
        );

        when(clienteService.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/v1/clientes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id")
                        .value(response.id().toString()))
                .andExpect(jsonPath("$.content[0].nome")
                        .value("Joao da Silva"))
                .andExpect(jsonPath("$.content[0].organizacaoId")
                        .value(organizacaoId.toString()));
    }

   @Test
    void deveBuscarClientesPorNome() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = criarResponse(
                UUID.randomUUID(),
                organizacaoId,
                true
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(clienteService.buscarPorNome(
                eq("Joao"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/clientes")
                        .param("nome", "Joao")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome")
                        .value("Joao da Silva"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }

    @Test
    void deveListarClientesPorStatus() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = criarResponse(
                UUID.randomUUID(),
                organizacaoId,
                false
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(clienteService.listarPorStatus(
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/clientes")
                        .param("ativo", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ativo")
                        .value(false))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }
    
    @Test
    void deveBuscarClientePorId() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        when(clienteService.buscarPorId(clienteId))
                .thenReturn(criarResponse(
                        clienteId,
                        organizacaoId,
                        true
                ));

        mockMvc.perform(get(
                        "/api/v1/clientes/{id}",
                        clienteId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(clienteId.toString()))
                .andExpect(jsonPath("$.organizacaoId")
                        .value(organizacaoId.toString()));
    }

    @Test
    void deveRetornarNotFoundParaClienteInexistente()
            throws Exception {

        UUID clienteId = UUID.randomUUID();

        when(clienteService.buscarPorId(clienteId))
                .thenThrow(new RecursoNaoEncontradoException(
                        "Cliente nao encontrado"
                ));

        mockMvc.perform(get(
                        "/api/v1/clientes/{id}",
                        clienteId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Cliente nao encontrado"));
    }

    @Test
    void deveAtualizarCliente() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = new ClienteResponse(
                clienteId,
                TipoPessoa.FISICA,
                "Joao Atualizado",
                null,
                "52998224725",
                "novo@email.com",
                "4133335555",
                "41999997777",
                "Dados atualizados",
                true,
                organizacaoId,
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T21:00:00Z")
        );

        when(clienteService.atualizar(
                eq(clienteId),
                any(AtualizarClienteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/api/v1/clientes/{id}",
                        clienteId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipoPessoa": "FISICA",
                              "nome": "Joao Atualizado",
                              "razaoSocial": null,
                              "cpfCnpj": "52998224725",
                              "email": "novo@email.com",
                              "telefone": "4133335555",
                              "celular": "41999997777",
                              "observacoes": "Dados atualizados"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome")
                        .value("Joao Atualizado"))
                .andExpect(jsonPath("$.email")
                        .value("novo@email.com"));
    }

    @Test
    void deveAlterarStatusCliente() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        ClienteResponse response = criarResponse(
                clienteId,
                organizacaoId,
                false
        );

        when(clienteService.alterarStatus(
                eq(clienteId),
                any(AlterarStatusClienteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/clientes/{id}/status",
                        clienteId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ativo": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo")
                        .value(false));
    }

    @Test
    void deveRetornarBadRequestAoAlterarStatusSemValor()
            throws Exception {

        UUID clienteId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/clientes/{id}/status",
                        clienteId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ativo": null
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ERRO_VALIDACAO"));
    }

    private ClienteResponse criarResponse(
            UUID id,
            UUID organizacaoId,
            boolean ativo
    ) {
        return new ClienteResponse(
                id,
                TipoPessoa.FISICA,
                "Joao da Silva",
                null,
                "52998224725",
                "joao@email.com",
                "4133334444",
                "41999998888",
                "Cliente mensalista",
                ativo,
                organizacaoId,
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z")
        );
    }
}