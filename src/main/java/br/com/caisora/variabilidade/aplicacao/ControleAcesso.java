package br.com.caisora.variabilidade.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("acesso")
public class ControleAcesso {

    private final VariabilidadeService variabilidadeService;

    public ControleAcesso(VariabilidadeService variabilidadeService) {
        this.variabilidadeService = variabilidadeService;
    }

    public boolean permitido(Authentication authentication, String modulo, String acao) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        try {
            PerfilUsuario perfil = PerfilUsuario.valueOf(jwt.getClaimAsString("perfil"));
            if (perfil == PerfilUsuario.ADMINISTRADOR_PLATAFORMA) {
                return true;
            }

            UUID organizacaoId = UUID.fromString(jwt.getClaimAsString("organizacaoId"));
            return variabilidadeService.permitido(
                    organizacaoId,
                    perfil,
                    ModuloSistema.valueOf(modulo),
                    AcaoSistema.valueOf(acao)
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
