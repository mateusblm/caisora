package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.autenticacao.api.RespostaLogin;
import br.com.caisora.autenticacao.api.SolicitacaoLogin;
import br.com.caisora.autenticacao.api.UsuarioAutenticadoResponse;
import br.com.caisora.compartilhado.excecao.CredenciaisInvalidasException;
import br.com.caisora.compartilhado.excecao.OrganizacaoInativaException;
import br.com.caisora.compartilhado.excecao.UsuarioInativoException;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorTokenJwt geradorTokenJwt;
    private final LeitorTokenJwt leitorTokenJwt;
    private final AutenticacaoMapper autenticacaoMapper;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            GeradorTokenJwt geradorTokenJwt,
            LeitorTokenJwt leitorTokenJwt,
            AutenticacaoMapper autenticacaoMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.geradorTokenJwt = geradorTokenJwt;
        this.leitorTokenJwt = leitorTokenJwt;
        this.autenticacaoMapper = autenticacaoMapper;
    }

    /**
     * Autentica por organizacao, e-mail e senha. A organizacao ainda chega por
     * header no MVP; futuramente sera resolvida pelo subdominio antes de validar
     * credenciais. Usuario ou organizacao inativos nao recebem token.
     */
    @Transactional(readOnly = true)
    public RespostaLogin autenticar(UUID organizacaoId, SolicitacaoLogin solicitacao) {
        Usuario usuario = usuarioRepository
                .findByOrganizacaoIdAndEmail(organizacaoId, normalizarEmail(solicitacao.email()))
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(solicitacao.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException();
        }

        if (!usuario.getOrganizacao().isAtiva()) {
            throw new OrganizacaoInativaException();
        }

        return new RespostaLogin(
                geradorTokenJwt.gerarToken(usuario),
                "Bearer",
                geradorTokenJwt.obterExpiracaoSegundos(),
                autenticacaoMapper.paraResponse(usuario));
    }

    public UsuarioAutenticadoResponse obterUsuarioAtual() {
        return autenticacaoMapper.paraResponse(leitorTokenJwt.obterUsuarioAutenticado());
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
