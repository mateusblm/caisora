package br.com.caisora.autenticacao.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.autenticacao.aplicacao.AutenticacaoService;
import br.com.caisora.compartilhado.excecao.CredenciaisInvalidasException;
import br.com.caisora.compartilhado.excecao.OrganizacaoInativaException;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.compartilhado.excecao.UsuarioInativoException;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AutenticacaoControllerTest {

    @Mock
    private AutenticacaoService autenticacaoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AutenticacaoController controller = new AutenticacaoController(autenticacaoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TratadorGlobalException())
                .setValidator(validator)
                .build();
    }

    @Test
    void deveAutenticarComSucesso() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(autenticacaoService.autenticar(any(SolicitacaoLogin.class)))
                .thenReturn(new RespostaLogin(
                        "token.jwt",
                        "Bearer",
                        3600,
                        new UsuarioAutenticadoResponse(
                                usuarioId,
                                "Maria Silva",
                                "maria@marina.com",
                                PerfilUsuario.ADMINISTRADOR_MARINA,
                                organizacaoId,
                                "Marina Teste")));

        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "marina-teste",
                                  "email": "maria@marina.com",
                                  "senha": "SenhaForte123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcesso").value("token.jwt"))
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.usuario.id").value(usuarioId.toString()));
    }

    @Test
    void deveRetornarBadRequestQuandoCodigoOrganizacaoNaoForInformado() throws Exception {
        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "",
                                  "email": "maria@marina.com",
                                  "senha": "SenhaForte123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
    }

    @Test
    void deveRetornarBadRequestQuandoLoginForInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "marina-teste",
                                  "email": "email-invalido",
                                  "senha": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
    }

    @Test
    void deveRetornarUnauthorizedQuandoCredenciaisForemInvalidas() throws Exception {
        when(autenticacaoService.autenticar(any(SolicitacaoLogin.class)))
                .thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "marina-teste",
                                  "email": "maria@marina.com",
                                  "senha": "senha-errada"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void deveRetornarForbiddenQuandoUsuarioEstiverInativo() throws Exception {
        when(autenticacaoService.autenticar(any(SolicitacaoLogin.class)))
                .thenThrow(new UsuarioInativoException());

        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "marina-teste",
                                  "email": "maria@marina.com",
                                  "senha": "SenhaForte123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("USUARIO_INATIVO"));
    }

    @Test
    void deveRetornarForbiddenQuandoOrganizacaoEstiverInativa() throws Exception {
        when(autenticacaoService.autenticar(any(SolicitacaoLogin.class)))
                .thenThrow(new OrganizacaoInativaException());

        mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoOrganizacao": "marina-teste",
                                  "email": "maria@marina.com",
                                  "senha": "SenhaForte123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORGANIZACAO_INATIVA"));
    }

    @Test
    void deveRetornarUsuarioAtual() throws Exception {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(autenticacaoService.obterUsuarioAtual())
                .thenReturn(new UsuarioAutenticadoResponse(
                        usuarioId,
                        "Maria Silva",
                        "maria@marina.com",
                        PerfilUsuario.ADMINISTRADOR_MARINA,
                        organizacaoId,
                        "Marina Teste"));

        mockMvc.perform(get("/api/v1/autenticacao/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId.toString()))
                .andExpect(jsonPath("$.organizacaoId").value(organizacaoId.toString()));
    }
}
