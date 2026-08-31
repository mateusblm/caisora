package br.com.caisora.variabilidade.api;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.aplicacao.VariabilidadeService;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/variabilidade")
public class VariabilidadeController {

    private final VariabilidadeService variabilidadeService;
    private final LeitorTokenJwt leitorTokenJwt;

    public VariabilidadeController(
            VariabilidadeService variabilidadeService,
            LeitorTokenJwt leitorTokenJwt
    ) {
        this.variabilidadeService = variabilidadeService;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @GetMapping("/minha-configuracao")
    public MinhaConfiguracaoResponse minhaConfiguracao() {
        return variabilidadeService.obterMinhaConfiguracao(leitorTokenJwt.obterUsuarioAutenticado());
    }

    @GetMapping("/configuracao")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_MARINA','ADMINISTRADOR_PLATAFORMA')")
    public ConfiguracaoVariabilidadeResponse configuracao() {
        UsuarioAutenticado usuario = leitorTokenJwt.obterUsuarioAutenticado();
        return variabilidadeService.obterConfiguracao(usuario.organizacaoId());
    }

    @PutMapping("/modulos/{modulo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_MARINA','ADMINISTRADOR_PLATAFORMA')")
    public ConfiguracaoVariabilidadeResponse atualizarModulo(
            @PathVariable ModuloSistema modulo,
            @Valid @RequestBody AtualizarModuloRequest request
    ) {
        UsuarioAutenticado usuario = leitorTokenJwt.obterUsuarioAutenticado();
        return variabilidadeService.atualizarModulo(
                usuario.organizacaoId(),
                modulo,
                request.ativo()
        );
    }

    @PutMapping("/perfis/{perfil}/permissoes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_MARINA','ADMINISTRADOR_PLATAFORMA')")
    public ConfiguracaoVariabilidadeResponse atualizarPermissoes(
            @PathVariable PerfilUsuario perfil,
            @Valid @RequestBody AtualizarPermissoesPerfilRequest request
    ) {
        UsuarioAutenticado usuario = leitorTokenJwt.obterUsuarioAutenticado();
        return variabilidadeService.atualizarPermissoes(
                usuario.organizacaoId(),
                perfil,
                request.permissoes()
        );
    }
}
