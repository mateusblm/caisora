package br.com.caisora.organizacao.api;

import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/minha-organizacao/coordenadas")
public class CoordenadasController {
    private final OrganizacaoService service;
    private final LeitorTokenJwt leitor;
    public CoordenadasController(OrganizacaoService service, LeitorTokenJwt leitor) {
        this.service=service;
        this.leitor=leitor;
    }
    @GetMapping public CoordenadasResponse consultar() {
        return service.coordenadas(leitor.obterUsuarioAutenticado().organizacaoId());
    }
    @PutMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR_MARINA','ADMINISTRADOR_PLATAFORMA')")
    public CoordenadasResponse configurar(@Valid @RequestBody CoordenadasRequest request) {
        return service.configurarCoordenadas(leitor.obterUsuarioAutenticado().organizacaoId(),request);
    }
}
