package br.com.caisora.variabilidade.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.Set;

public record PerfilConfiguracaoResponse(
        PerfilUsuario perfil,
        Set<String> permissoes
) {
}
