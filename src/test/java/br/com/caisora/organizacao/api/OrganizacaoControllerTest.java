package br.com.caisora.organizacao.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrganizacaoControllerTest {

    @Mock
    private OrganizacaoService organizacaoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        OrganizacaoController controller = new OrganizacaoController(organizacaoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TratadorGlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void deveCriarOrganizacao() throws Exception {
        OrganizacaoResponse response = criarResponse(UUID.randomUUID());

        when(organizacaoService.criar(any(CriarOrganizacaoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Marina Teste",
                                  "slug": "marina-teste",
                                  "razaoSocial": "Marina Teste LTDA",
                                  "documento": "12345678000199",
                                  "email": "contato@marinateste.com",
                                  "telefone": "11999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.nome").value("Marina Teste"))
                .andExpect(jsonPath("$.slug").value("marina-teste"))
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void deveRetornarBadRequestAoCriarOrganizacaoInvalida() throws Exception {
        mockMvc.perform(post("/api/v1/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "email-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"))
                .andExpect(jsonPath("$.errosCampos").isArray());
    }

    @Test
    void deveListarOrganizacoes() throws Exception {
        OrganizacaoResponse response = criarResponse(UUID.randomUUID());
        when(organizacaoService.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/organizacoes?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                .andExpect(jsonPath("$.content[0].nome").value("Marina Teste"));
    }

    @Test
    void deveBuscarOrganizacaoPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizacaoService.buscarPorId(id)).thenReturn(criarResponse(id));

        mockMvc.perform(get("/api/v1/organizacoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("contato@marinateste.com"));
    }

    @Test
    void deveAtualizarOrganizacao() throws Exception {
        UUID id = UUID.randomUUID();

        when(organizacaoService.atualizar(eq(id), any(AtualizarOrganizacaoRequest.class)))
                .thenReturn(criarResponse(id));

        mockMvc.perform(put("/api/v1/organizacoes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Marina Teste",
                                  "razaoSocial": "Marina Teste LTDA",
                                  "documento": "12345678000199",
                                  "email": "contato@marinateste.com",
                                  "telefone": "11999999999",
                                  "ativa": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deveInativarOrganizacao() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(organizacaoService).inativar(id);

        mockMvc.perform(delete("/api/v1/organizacoes/{id}", id))
                .andExpect(status().isNoContent());
    }

    private OrganizacaoResponse criarResponse(UUID id) {
        return new OrganizacaoResponse(
                id,
                "Marina Teste",
                "marina-teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999",
                true,
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z"));
    }
}
