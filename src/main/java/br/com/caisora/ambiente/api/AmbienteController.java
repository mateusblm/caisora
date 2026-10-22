package br.com.caisora.ambiente.api;

import br.com.caisora.ambiente.aplicacao.AmbienteService;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ambiente")
@PreAuthorize("@acesso.permitido(authentication, 'AMBIENTE', 'VISUALIZAR')")
public class AmbienteController {
    private final AmbienteService service;
    private final LeitorTokenJwt leitor;
    public AmbienteController(AmbienteService service, LeitorTokenJwt leitor) {
        this.service=service;
        this.leitor=leitor;
    }
    @GetMapping("/meteorologia") public PrevisaoResponse meteorologia(@RequestParam LocalDate inicio,@RequestParam LocalDate fim) {
        return service.consultar(leitor.obterUsuarioAutenticado().organizacaoId(),false,inicio,fim);
    }
    @GetMapping("/maritimo") public PrevisaoResponse maritimo(@RequestParam LocalDate inicio,@RequestParam LocalDate fim) {
        return service.consultar(leitor.obterUsuarioAutenticado().organizacaoId(),true,inicio,fim);
    }
}
