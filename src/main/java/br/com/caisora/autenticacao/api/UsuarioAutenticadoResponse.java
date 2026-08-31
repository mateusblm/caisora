package br.com.caisora.autenticacao.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.Set;
import java.util.UUID;

public record UsuarioAutenticadoResponse(
        UUID id,
        String nome,
        String email,
        PerfilUsuario perfil,
        UUID organizacaoId,
        String organizacaoNome,
        Set<ModuloSistema> modulosAtivos,
        Set<String> permissoes
) {
    public UsuarioAutenticadoResponse(
            UUID id, String nome, String email, PerfilUsuario perfil,
            UUID organizacaoId, String organizacaoNome
    ) {
        this(id, nome, email, perfil, organizacaoId, organizacaoNome, Set.of(), Set.of());
    }
}
