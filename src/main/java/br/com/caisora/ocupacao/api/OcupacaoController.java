package br.com.caisora.ocupacao.api;

import br.com.caisora.ocupacao.aplicacao.OcupacaoService;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ocupacoes")
public class OcupacaoController {

    private final OcupacaoService ocupacaoService;

    public OcupacaoController(
        OcupacaoService ocupacaoService
    ) {
        this.ocupacaoService =
            ocupacaoService;
    }

    @PostMapping
    public ResponseEntity<OcupacaoResponse> criar(
        @Valid
        @RequestBody
        CriarOcupacaoRequest request
    ) {
        OcupacaoResponse ocupacao =
            ocupacaoService.criar(request);

        URI localizacao = URI.create(
            "/api/v1/ocupacoes/"
                + ocupacao.id()
        );

        return ResponseEntity
            .created(localizacao)
            .body(ocupacao);
    }

    @GetMapping
    public Page<OcupacaoResponse> listar(
        @RequestParam(required = false)
        UUID embarcacaoId,

        @RequestParam(required = false)
        UUID vagaId,

        @RequestParam(required = false)
        StatusOcupacao status,

        @PageableDefault(
            size = 20,
            sort = "inicioEm",
            direction = Sort.Direction.DESC
        )
        Pageable paginacao
    ) {
        if (embarcacaoId != null) {
            return ocupacaoService
                .listarPorEmbarcacao(
                    embarcacaoId,
                    paginacao
                );
        }

        if (vagaId != null) {
            return ocupacaoService
                .listarPorVaga(
                    vagaId,
                    paginacao
                );
        }

        if (status != null) {
            return ocupacaoService
                .listarPorStatus(
                    status,
                    paginacao
                );
        }

        return ocupacaoService.listar(
            paginacao
        );
    }

    @GetMapping("/{id}")
    public OcupacaoResponse buscarPorId(
        @PathVariable
        UUID id
    ) {
        return ocupacaoService.buscarPorId(
            id
        );
    }

    @PutMapping("/{id}")
    public OcupacaoResponse atualizar(
        @PathVariable
        UUID id,

        @Valid
        @RequestBody
        AtualizarOcupacaoRequest request
    ) {
        return ocupacaoService.atualizar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/encerramento")
    public OcupacaoResponse encerrar(
        @PathVariable
        UUID id,

        @Valid
        @RequestBody
        EncerrarOcupacaoRequest request
    ) {
        return ocupacaoService.encerrar(
            id,
            request
        );
    }
}
