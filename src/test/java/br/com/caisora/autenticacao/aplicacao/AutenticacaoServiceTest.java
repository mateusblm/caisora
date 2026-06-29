package br.com.caisora.autenticacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.api.SolicitacaoLogin;
import br.com.caisora.compartilhado.excecao.CredenciaisInvalidasException;
import br.com.caisora.compartilhado.excecao.OrganizacaoInativaException;
import br.com.caisora.compartilhado.excecao.UsuarioInativoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GeradorTokenJwt geradorTokenJwt;

    @Mock
    private LeitorTokenJwt leitorTokenJwt;

    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void configurar() {
        autenticacaoService = new AutenticacaoService(
                usuarioRepository,
                organizacaoRepository,
                passwordEncoder,
                geradorTokenJwt,
                leitorTokenJwt,
                new AutenticacaoMapper());
    }

    @Test
    void deveAutenticarUsuarioAtivoDeOrganizacaoAtiva() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(organizacaoId, true, true);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-teste", "MARIA@MARINA.COM", "SenhaForte123");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(usuario.getOrganizacao()));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte123", "hash-bcrypt")).thenReturn(true);
        when(geradorTokenJwt.gerarToken(usuario)).thenReturn("token.jwt");
        when(geradorTokenJwt.obterExpiracaoSegundos()).thenReturn(3600L);

        var resposta = autenticacaoService.autenticar(solicitacao);

        assertThat(resposta.tokenAcesso()).isEqualTo("token.jwt");
        assertThat(resposta.tipoToken()).isEqualTo("Bearer");
        assertThat(resposta.expiraEm()).isEqualTo(3600L);
        assertThat(resposta.usuario().email()).isEqualTo("maria@marina.com");
        assertThat(resposta.usuario().organizacaoId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveAutenticarNormalizandoCodigoOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(organizacaoId, true, true);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin(" MARINA-TESTE ", "MARIA@MARINA.COM", "SenhaForte123");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(usuario.getOrganizacao()));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte123", "hash-bcrypt")).thenReturn(true);
        when(geradorTokenJwt.gerarToken(usuario)).thenReturn("token.jwt");
        when(geradorTokenJwt.obterExpiracaoSegundos()).thenReturn(3600L);

        var resposta = autenticacaoService.autenticar(solicitacao);

        assertThat(resposta.usuario().organizacaoId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharQuandoUsuarioNaoExisteNaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(organizacaoId, true);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-teste", "maria@marina.com", "SenhaForte123");
        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(organizacao));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacaoService.autenticar(solicitacao))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Codigo da marina, e-mail ou senha invalidos");
    }

    @Test
    void deveFalharQuandoOrganizacaoNaoExistir() {
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-inexistente", "maria@marina.com", "SenhaForte123");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-inexistente"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacaoService.autenticar(solicitacao))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Codigo da marina, e-mail ou senha invalidos");
    }

    @Test
    void deveFalharQuandoSenhaForInvalida() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(organizacaoId, true, true);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-teste", "maria@marina.com", "senha-errada");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(usuario.getOrganizacao()));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> autenticacaoService.autenticar(solicitacao))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Codigo da marina, e-mail ou senha invalidos");
    }

    @Test
    void deveFalharQuandoUsuarioEstiverInativo() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(organizacaoId, true, false);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-teste", "maria@marina.com", "SenhaForte123");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(usuario.getOrganizacao()));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte123", "hash-bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> autenticacaoService.autenticar(solicitacao))
                .isInstanceOf(UsuarioInativoException.class)
                .hasMessage("Usuario inativo");
    }

    @Test
    void deveFalharQuandoOrganizacaoEstiverInativa() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(organizacaoId, false, true);
        SolicitacaoLogin solicitacao = new SolicitacaoLogin("marina-teste", "maria@marina.com", "SenhaForte123");

        when(organizacaoRepository.findBySlugIgnoreCase("marina-teste"))
                .thenReturn(Optional.of(usuario.getOrganizacao()));
        when(usuarioRepository.findByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte123", "hash-bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> autenticacaoService.autenticar(solicitacao))
                .isInstanceOf(OrganizacaoInativaException.class)
                .hasMessage("Organizacao inativa");
    }

    @Test
    void deveRetornarUsuarioAtualAPartirDoJwtValidado() {
        UUID usuarioId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
                usuarioId,
                "Maria Silva",
                "maria@marina.com",
                PerfilUsuario.ADMINISTRADOR_MARINA,
                organizacaoId,
                "Marina Teste");

        when(leitorTokenJwt.obterUsuarioAutenticado()).thenReturn(usuarioAutenticado);

        var response = autenticacaoService.obterUsuarioAtual();

        assertThat(response.id()).isEqualTo(usuarioId);
        assertThat(response.organizacaoId()).isEqualTo(organizacaoId);
        assertThat(response.perfil()).isEqualTo(PerfilUsuario.ADMINISTRADOR_MARINA);
    }

    private Usuario criarUsuarioPersistido(UUID organizacaoId, boolean organizacaoAtiva, boolean usuarioAtivo) {
        Organizacao organizacao = Organizacao.criar(
                "Marina Teste",
                "marina-teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999");
        ReflectionTestUtils.setField(organizacao, "id", organizacaoId);
        ReflectionTestUtils.setField(organizacao, "ativa", organizacaoAtiva);
        ReflectionTestUtils.setField(organizacao, "criadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(organizacao, "atualizadaEm", Instant.parse("2026-06-28T20:00:00Z"));

        Usuario usuario = Usuario.criar(
                organizacao,
                "Maria Silva",
                "maria@marina.com",
                "hash-bcrypt",
                PerfilUsuario.ADMINISTRADOR_MARINA);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(usuario, "ativo", usuarioAtivo);
        ReflectionTestUtils.setField(usuario, "criadoEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(usuario, "atualizadoEm", Instant.parse("2026-06-28T20:00:00Z"));
        return usuario;
    }

    private Organizacao criarOrganizacaoPersistida(UUID organizacaoId, boolean organizacaoAtiva) {
        Organizacao organizacao = Organizacao.criar(
                "Marina Teste",
                "marina-teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999");
        ReflectionTestUtils.setField(organizacao, "id", organizacaoId);
        ReflectionTestUtils.setField(organizacao, "ativa", organizacaoAtiva);
        ReflectionTestUtils.setField(organizacao, "criadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(organizacao, "atualizadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        return organizacao;
    }
}
