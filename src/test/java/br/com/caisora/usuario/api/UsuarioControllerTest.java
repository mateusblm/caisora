package br.com.caisora.usuario.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.usuario.aplicacao.UsuarioService;
import br.com.caisora.usuario.dominio.PerfilUsuario;
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
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        UsuarioController controller = new UsuarioController(usuarioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TratadorGlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void deveCriarUsuario() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UsuarioResponse response = criarResponse(UUID.randomUUID(), organizacaoId);
        when(usuarioService.criar(eq(organizacaoId), any(CriarUsuarioRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria Silva",
                                  "email": "maria@marina.com",
                                  "senha": "SenhaForte123",
                                  "perfil": "ATENDENTE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.email").value("maria@marina.com"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void deveRetornarBadRequestAoCriarUsuarioInvalido() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "email-invalido",
                                  "senha": "curta"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"))
                .andExpect(jsonPath("$.errosCampos").isArray());
    }

    @Test
    void deveListarUsuarios() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UsuarioResponse response = criarResponse(UUID.randomUUID(), organizacaoId);
        when(usuarioService.listar(eq(organizacaoId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/usuarios?page=0&size=10")
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                .andExpect(jsonPath("$.content[0].organizacaoId").value(organizacaoId.toString()));
    }

    @Test
    void deveBuscarUsuarioPorId() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(usuarioService.buscarPorId(organizacaoId, usuarioId)).thenReturn(criarResponse(usuarioId, organizacaoId));

        mockMvc.perform(get("/api/v1/usuarios/{id}", usuarioId)
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId.toString()))
                .andExpect(jsonPath("$.organizacaoId").value(organizacaoId.toString()));
    }

    @Test
    void deveAtualizarUsuario() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UsuarioResponse response = new UsuarioResponse(
                usuarioId,
                "Maria Atualizada",
                "maria@marina.com",
                PerfilUsuario.GERENTE,
                true,
                organizacaoId,
                "Marina Teste",
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z"));
        when(usuarioService.atualizar(eq(organizacaoId), eq(usuarioId), any(AtualizarUsuarioRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/usuarios/{id}", usuarioId)
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria Atualizada",
                                  "perfil": "GERENTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Atualizada"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));
    }

    @Test
    void deveAlterarStatusUsuario() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UsuarioResponse response = new UsuarioResponse(
                usuarioId,
                "Maria Silva",
                "maria@marina.com",
                PerfilUsuario.ATENDENTE,
                false,
                organizacaoId,
                "Marina Teste",
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z"));
        when(usuarioService.alterarStatus(eq(organizacaoId), eq(usuarioId), any(AlterarStatusUsuarioRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/usuarios/{id}/status", usuarioId)
                        .header(UsuarioController.HEADER_ORGANIZACAO_ID, organizacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    private UsuarioResponse criarResponse(UUID id, UUID organizacaoId) {
        return new UsuarioResponse(
                id,
                "Maria Silva",
                "maria@marina.com",
                PerfilUsuario.ATENDENTE,
                true,
                organizacaoId,
                "Marina Teste",
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z"));
    }
}
