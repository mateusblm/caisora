package br.com.caisora.usuario.aplicacao;

import br.com.caisora.usuario.api.UsuarioResponse;
import br.com.caisora.usuario.dominio.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getOrganizacao().getId(),
                usuario.getOrganizacao().getNome(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
