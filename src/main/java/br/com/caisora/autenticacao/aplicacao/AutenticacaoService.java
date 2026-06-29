package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.autenticacao.api.RespostaLogin;
import br.com.caisora.autenticacao.api.SolicitacaoLogin;
import br.com.caisora.autenticacao.api.UsuarioAutenticadoResponse;
import br.com.caisora.compartilhado.excecao.CredenciaisInvalidasException;
import br.com.caisora.compartilhado.excecao.OrganizacaoInativaException;
import br.com.caisora.compartilhado.excecao.UsuarioInativoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.organizacao.dominio.SlugOrganizacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorTokenJwt geradorTokenJwt;
    private final LeitorTokenJwt leitorTokenJwt;
    private final AutenticacaoMapper autenticacaoMapper;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            GeradorTokenJwt geradorTokenJwt,
            LeitorTokenJwt leitorTokenJwt,
            AutenticacaoMapper autenticacaoMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.geradorTokenJwt = geradorTokenJwt;
        this.leitorTokenJwt = leitorTokenJwt;
        this.autenticacaoMapper = autenticacaoMapper;
    }

    /**
     * Autentica por codigo da marina, e-mail e senha. O codigo e normalizado e
     * usado apenas para localizar a organizacao; o UUID segue sendo usado nos
     * relacionamentos, isolamento multi-tenant e claims do JWT.
     */
    @Transactional(readOnly = true)
    public RespostaLogin autenticar(SolicitacaoLogin solicitacao) {
        Organizacao organizacao = buscarOrganizacaoPorSlug(solicitacao.codigoOrganizacao());

        Usuario usuario = usuarioRepository
                .findByOrganizacaoIdAndEmail(organizacao.getId(), normalizarEmail(solicitacao.email()))
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

    private Organizacao buscarOrganizacaoPorSlug(String codigoOrganizacao) {
        try {
            String slug = SlugOrganizacao.normalizar(codigoOrganizacao);

            return organizacaoRepository.findBySlugIgnoreCase(slug)
                    .orElseThrow(CredenciaisInvalidasException::new);
        } catch (IllegalArgumentException exception) {
            throw new CredenciaisInvalidasException();
        }
    }
}
