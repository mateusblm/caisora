package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.aplicacao.PosicaoEmbarcacaoService;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PosicaoEmbarcacaoController {

    private final PosicaoEmbarcacaoService
        posicaoService;

    public PosicaoEmbarcacaoController(
        PosicaoEmbarcacaoService
            posicaoService
    ) {
        this.posicaoService = posicaoService;
    }

    @GetMapping(
        "/api/v1/embarcacoes/"
            + "{embarcacaoId}/posicao"
    )
    public PosicaoEmbarcacaoResponse
    buscarPorEmbarcacao(
        @PathVariable UUID embarcacaoId
    ) {
        return posicaoService
            .buscarPorEmbarcacao(
                embarcacaoId
            );
    }

    @GetMapping(
        "/api/v1/posicoes-embarcacoes"
    )
    public Page<PosicaoEmbarcacaoResponse>
    listar(
        @RequestParam(required = false)
        TipoPosicaoEmbarcacao tipo,

        @PageableDefault(
            size = 20,
            sort = "atualizadaEm",
            direction = Sort.Direction.DESC
        )
        Pageable paginacao
    ) {
        return posicaoService.listar(
            tipo,
            paginacao
        );
    }
}
