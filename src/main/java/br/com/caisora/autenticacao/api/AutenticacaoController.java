package br.com.caisora.autenticacao.api;

import br.com.caisora.autenticacao.aplicacao.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public RespostaLogin autenticar(
            @Valid @RequestBody SolicitacaoLogin solicitacao
    ) {
        return autenticacaoService.autenticar(solicitacao);
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse obterUsuarioAtual() {
        return autenticacaoService.obterUsuarioAtual();
    }
}
