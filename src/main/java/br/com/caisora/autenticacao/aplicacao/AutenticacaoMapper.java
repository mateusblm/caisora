package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.autenticacao.api.UsuarioAutenticadoResponse;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.variabilidade.api.MinhaConfiguracaoResponse;
import br.com.caisora.variabilidade.aplicacao.VariabilidadeService;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class AutenticacaoMapper {

    private final VariabilidadeService variabilidadeService;

    public AutenticacaoMapper() {
        this.variabilidadeService = null;
    }

    public AutenticacaoMapper(VariabilidadeService variabilidadeService) {
        this.variabilidadeService = variabilidadeService;
    }

    public UsuarioAutenticadoResponse paraResponse(Usuario usuario) {
        MinhaConfiguracaoResponse configuracao = configuracao(usuario.getOrganizacao().getId(), usuario.getPerfil());
        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getOrganizacao().getId(),
                usuario.getOrganizacao().getNome(), configuracao.modulosAtivos(), configuracao.permissoes());
    }

    public UsuarioAutenticadoResponse paraResponse(UsuarioAutenticado usuario) {
        MinhaConfiguracaoResponse configuracao = configuracao(usuario.organizacaoId(), usuario.perfil());
        return new UsuarioAutenticadoResponse(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.perfil(),
                usuario.organizacaoId(),
                usuario.organizacaoNome(), configuracao.modulosAtivos(), configuracao.permissoes());
    }

    private MinhaConfiguracaoResponse configuracao(java.util.UUID organizacaoId,
            br.com.caisora.usuario.dominio.PerfilUsuario perfil) {
        return variabilidadeService == null
                ? new MinhaConfiguracaoResponse(Set.of(), Set.of())
                : variabilidadeService.obterMinhaConfiguracao(organizacaoId, perfil);
    }
}
