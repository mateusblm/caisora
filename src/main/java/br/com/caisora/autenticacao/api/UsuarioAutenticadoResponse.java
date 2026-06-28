package br.com.caisora.autenticacao.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.UUID;

public record UsuarioAutenticadoResponse(
        UUID id,
        String nome,
        String email,
        PerfilUsuario perfil,
        UUID organizacaoId,
        String organizacaoNome
) {
}
