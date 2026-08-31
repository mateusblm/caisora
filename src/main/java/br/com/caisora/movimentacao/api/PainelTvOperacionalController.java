package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.aplicacao.PainelTvOperacionalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("@acesso.permitido(authentication, 'PAINEL_TV', 'VISUALIZAR')")
@RequestMapping("/api/v1/movimentacoes")
public class PainelTvOperacionalController {

    private final PainelTvOperacionalService
        painelTvOperacionalService;

    public PainelTvOperacionalController(
        PainelTvOperacionalService
            painelTvOperacionalService
    ) {
        this.painelTvOperacionalService =
            painelTvOperacionalService;
    }

    @GetMapping("/painel-tv")
    public PainelTvOperacionalResponse buscar() {
        return painelTvOperacionalService.buscar();
    }
}
