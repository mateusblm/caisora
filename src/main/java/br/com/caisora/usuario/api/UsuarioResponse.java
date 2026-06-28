package br.com.caisora.usuario.api;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        PerfilUsuario perfil,
        boolean ativo,
        UUID organizacaoId,
        String organizacaoNome,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
