package br.com.caisora.usuario.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.api.AlterarStatusUsuarioRequest;
import br.com.caisora.usuario.api.AtualizarUsuarioRequest;
import br.com.caisora.usuario.api.CriarUsuarioRequest;
import br.com.caisora.usuario.api.UsuarioResponse;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.time.Instant;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService(
                usuarioRepository,
                organizacaoRepository,
                passwordEncoder,
                new UsuarioMapper());
    }

    @Test
    void deveCriarUsuarioAtivoComEmailNormalizadoESenhaCriptografada() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(organizacaoId);
        CriarUsuarioRequest request = new CriarUsuarioRequest(
                "Maria Silva",
                "MARIA@MARINA.COM",
                "SenhaForte123",
                PerfilUsuario.ATENDENTE);

        when(organizacaoRepository.findById(organizacaoId)).thenReturn(Optional.of(organizacao));
        when(usuarioRepository.existsByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            popularAuditoria(usuario, UUID.randomUUID());
            return usuario;
        });

        UsuarioResponse response = usuarioService.criar(organizacaoId, request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("maria@marina.com");
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(captor.getValue().isAtivo()).isTrue();
        assertThat(response.email()).isEqualTo("maria@marina.com");
        assertThat(response.ativo()).isTrue();
        assertThat(response.organizacaoId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoCriarUsuarioComEmailDuplicadoNaMesmaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        CriarUsuarioRequest request = new CriarUsuarioRequest(
                "Maria Silva",
                "maria@marina.com",
                "SenhaForte123",
                PerfilUsuario.ATENDENTE);

        when(organizacaoRepository.findById(organizacaoId)).thenReturn(Optional.of(criarOrganizacaoPersistida(organizacaoId)));
        when(usuarioRepository.existsByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(organizacaoId, request))
                .isInstanceOf(ConflitoDadosException.class)
                .hasMessage("Ja existe usuario com este e-mail na organizacao");
    }

    @Test
    void devePermitirMesmoEmailEmOrganizacoesDiferentes() {
        UUID organizacaoId = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(organizacaoId);
        CriarUsuarioRequest request = new CriarUsuarioRequest(
                "Maria Silva",
                "maria@marina.com",
                "SenhaForte123",
                PerfilUsuario.ATENDENTE);

        when(organizacaoRepository.findById(organizacaoId)).thenReturn(Optional.of(organizacao));
        when(usuarioRepository.existsByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            popularAuditoria(usuario, UUID.randomUUID());
            return usuario;
        });

        UsuarioResponse response = usuarioService.criar(organizacaoId, request);

        assertThat(response.email()).isEqualTo("maria@marina.com");
        verify(usuarioRepository).existsByOrganizacaoIdAndEmail(organizacaoId, "maria@marina.com");
    }

    @Test
    void deveListarSomenteUsuariosDaOrganizacaoInformada() {
        UUID organizacaoId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(UUID.randomUUID(), criarOrganizacaoPersistida(organizacaoId));
        PageRequest paginacao = PageRequest.of(0, 10);
        when(usuarioRepository.findAllByOrganizacaoId(organizacaoId, paginacao))
                .thenReturn(new PageImpl<>(java.util.List.of(usuario), paginacao, 1));

        var pagina = usuarioService.listar(organizacaoId, paginacao);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).organizacaoId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveBuscarUsuarioPorIdDentroDaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(usuarioId, criarOrganizacaoPersistida(organizacaoId));
        when(usuarioRepository.findByIdAndOrganizacaoId(usuarioId, organizacaoId)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.buscarPorId(organizacaoId, usuarioId);

        assertThat(response.id()).isEqualTo(usuarioId);
        assertThat(response.organizacaoId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveFalharAoBuscarUsuarioDeOutraOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findByIdAndOrganizacaoId(usuarioId, organizacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(organizacaoId, usuarioId))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuario nao encontrado");
    }

    @Test
    void deveAtualizarUsuarioSemAlterarOrganizacaoOuSenha() {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(usuarioId, criarOrganizacaoPersistida(organizacaoId));
        when(usuarioRepository.findByIdAndOrganizacaoId(usuarioId, organizacaoId)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.atualizar(
                organizacaoId,
                usuarioId,
                new AtualizarUsuarioRequest("Maria Atualizada", PerfilUsuario.GERENTE));

        assertThat(response.nome()).isEqualTo("Maria Atualizada");
        assertThat(response.perfil()).isEqualTo(PerfilUsuario.GERENTE);
        assertThat(usuario.getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(usuario.getOrganizacao().getId()).isEqualTo(organizacaoId);
    }

    @Test
    void deveAlterarStatusUsuario() {
        UUID organizacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioPersistido(usuarioId, criarOrganizacaoPersistida(organizacaoId));
        when(usuarioRepository.findByIdAndOrganizacaoId(usuarioId, organizacaoId)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.alterarStatus(
                organizacaoId,
                usuarioId,
                new AlterarStatusUsuarioRequest(false));

        assertThat(response.ativo()).isFalse();
        assertThat(usuario.isAtivo()).isFalse();
    }

    private Organizacao criarOrganizacaoPersistida(UUID id) {
        Organizacao organizacao = Organizacao.criar(
                "Marina Teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999");
        ReflectionTestUtils.setField(organizacao, "id", id);
        ReflectionTestUtils.setField(organizacao, "criadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(organizacao, "atualizadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        return organizacao;
    }

    private Usuario criarUsuarioPersistido(UUID id, Organizacao organizacao) {
        Usuario usuario = Usuario.criar(
                organizacao,
                "Maria Silva",
                "maria@marina.com",
                "hash-bcrypt",
                PerfilUsuario.ATENDENTE);
        popularAuditoria(usuario, id);
        return usuario;
    }

    private void popularAuditoria(Usuario usuario, UUID id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        ReflectionTestUtils.setField(usuario, "criadoEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(usuario, "atualizadoEm", Instant.parse("2026-06-28T20:00:00Z"));
    }
}
