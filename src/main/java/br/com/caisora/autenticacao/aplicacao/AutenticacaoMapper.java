package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.autenticacao.api.UsuarioAutenticadoResponse;
import br.com.caisora.usuario.dominio.Usuario;
import org.springframework.stereotype.Component;

@Component
public class AutenticacaoMapper {

    public UsuarioAutenticadoResponse paraResponse(Usuario usuario) {
        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getOrganizacao().getId(),
                usuario.getOrganizacao().getNome());
    }

    public UsuarioAutenticadoResponse paraResponse(UsuarioAutenticado usuario) {
        return new UsuarioAutenticadoResponse(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.perfil(),
                usuario.organizacaoId(),
                usuario.organizacaoNome());
    }
}
