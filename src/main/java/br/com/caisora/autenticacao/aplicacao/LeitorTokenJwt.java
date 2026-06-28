package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class LeitorTokenJwt {

    /**
     * Le os claims do JWT ja validado pelo Spring Security Resource Server.
     * Neste ponto a assinatura e expiracao do token ja foram conferidas pela cadeia
     * de filtros, entao esta classe apenas traduz claims para o modelo da aplicacao.
     */
    public UsuarioAutenticado obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Usuario autenticado nao encontrado no contexto de seguranca");
        }

        return new UsuarioAutenticado(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("nome"),
                jwt.getClaimAsString("email"),
                PerfilUsuario.valueOf(jwt.getClaimAsString("perfil")),
                UUID.fromString(jwt.getClaimAsString("organizacaoId")),
                jwt.getClaimAsString("organizacaoNome"));
    }
}
