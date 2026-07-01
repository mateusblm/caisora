package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.aplicacao.PainelOperacionalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movimentacoes")
public class PainelOperacionalController {

    private final PainelOperacionalService painelOperacionalService;

    public PainelOperacionalController(
        PainelOperacionalService painelOperacionalService
    ) {
        this.painelOperacionalService = painelOperacionalService;
    }

    @GetMapping("/painel-operacional")
    public PainelOperacionalResponse buscar() {
        return painelOperacionalService.buscar();
    }
}
