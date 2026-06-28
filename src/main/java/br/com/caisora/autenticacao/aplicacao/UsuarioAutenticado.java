package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import java.util.UUID;

public record UsuarioAutenticado(
        UUID id,
        String nome,
        String email,
        PerfilUsuario perfil,
        UUID organizacaoId,
        String organizacaoNome
) {
}
