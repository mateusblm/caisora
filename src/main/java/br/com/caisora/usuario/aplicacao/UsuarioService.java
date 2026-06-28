package br.com.caisora.usuario.aplicacao;

import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.api.AlterarStatusUsuarioRequest;
import br.com.caisora.usuario.api.AtualizarUsuarioRequest;
import br.com.caisora.usuario.api.CriarUsuarioRequest;
import br.com.caisora.usuario.api.UsuarioResponse;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper usuarioMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    /**
     * Cria usuario dentro da organizacao informada pelo contexto temporario do MVP.
     * O e-mail e normalizado e validado como unico dentro da organizacao, permitindo
     * que outra organizacao use o mesmo e-mail sem quebrar o isolamento multi-tenant.
     */
    @Transactional
    public UsuarioResponse criar(UUID organizacaoId, CriarUsuarioRequest request) {
        Organizacao organizacao = buscarOrganizacao(organizacaoId);
        String emailNormalizado = normalizarEmail(request.email());
        validarEmailDisponivel(organizacaoId, emailNormalizado);

        Usuario usuario = Usuario.criar(
                organizacao,
                request.nome(),
                emailNormalizado,
                passwordEncoder.encode(request.senha()),
                request.perfil());

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponse(usuarioSalvo);
    }

    /**
     * Lista somente usuarios da organizacao atual. Esta regra evita vazamento entre
     * tenants mesmo enquanto a autenticacao JWT ainda nao existe.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(UUID organizacaoId, Pageable paginacao) {
        return usuarioRepository.findAllByOrganizacaoId(organizacaoId, paginacao)
                .map(usuarioMapper::paraResponse);
    }

    /**
     * Busca usuario sempre combinando id do usuario e id da organizacao. Se o usuario
     * existir em outro tenant, a resposta continua sendo "nao encontrado".
     */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID organizacaoId, UUID id) {
        return usuarioMapper.paraResponse(buscarEntidadePorId(organizacaoId, id));
    }

    @Transactional
    public UsuarioResponse atualizar(UUID organizacaoId, UUID id, AtualizarUsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(organizacaoId, id);
        usuario.atualizar(request.nome(), request.perfil());
        return usuarioMapper.paraResponse(usuario);
    }

    @Transactional
    public UsuarioResponse alterarStatus(UUID organizacaoId, UUID id, AlterarStatusUsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(organizacaoId, id);
        usuario.alterarStatus(request.ativo());
        return usuarioMapper.paraResponse(usuario);
    }

    private Organizacao buscarOrganizacao(UUID organizacaoId) {
        return organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organizacao nao encontrada"));
    }

    private Usuario buscarEntidadePorId(UUID organizacaoId, UUID id) {
        return usuarioRepository.findByIdAndOrganizacaoId(id, organizacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    private void validarEmailDisponivel(UUID organizacaoId, String emailNormalizado) {
        if (usuarioRepository.existsByOrganizacaoIdAndEmail(organizacaoId, emailNormalizado)) {
            throw new ConflitoDadosException("Ja existe usuario com este e-mail na organizacao");
        }
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
