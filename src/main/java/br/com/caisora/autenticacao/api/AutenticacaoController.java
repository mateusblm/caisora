package br.com.caisora.autenticacao.api;

import br.com.caisora.autenticacao.aplicacao.AutenticacaoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {

    public static final String HEADER_ORGANIZACAO_ID = "X-Organizacao-Id";

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    /**
     * MVP temporario: o header identifica a organizacao no login porque e-mail e
     * unico por organizacao, nao globalmente. No produto final, esse identificador
     * deve vir do subdominio da marina, mantendo o formulario com e-mail e senha.
     */
    @PostMapping("/login")
    public RespostaLogin autenticar(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            @Valid @RequestBody SolicitacaoLogin solicitacao
    ) {
        return autenticacaoService.autenticar(organizacaoId, solicitacao);
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse obterUsuarioAtual() {
        return autenticacaoService.obterUsuarioAtual();
    }
}
