package br.com.caisora.vaga.api;

import br.com.caisora.vaga.aplicacao.VagaService;
import br.com.caisora.vaga.dominio.TipoVaga;
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
@RequestMapping("/api/v1/vagas")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(
        VagaService vagaService
    ) {
        this.vagaService = vagaService;
    }

    @PostMapping
    public ResponseEntity<VagaResponse> criar(
        @Valid
        @RequestBody
        CriarVagaRequest request
    ) {
        VagaResponse vaga =
            vagaService.criar(request);

        URI localizacao = URI.create(
            "/api/v1/vagas/" + vaga.id()
        );

        return ResponseEntity
            .created(localizacao)
            .body(vaga);
    }

    @GetMapping
    public Page<VagaResponse> listar(
        @RequestParam(required = false)
        String codigo,

        @RequestParam(required = false)
        String setor,

        @RequestParam(required = false)
        TipoVaga tipo,

        @RequestParam(required = false)
        Boolean ativa,

        @PageableDefault(
            size = 20,
            sort = "codigo",
            direction = Sort.Direction.ASC
        )
        Pageable paginacao
    ) {
        if (
            codigo != null
                && !codigo.isBlank()
        ) {
            return vagaService.buscarPorCodigo(
                codigo,
                paginacao
            );
        }

        if (
            setor != null
                && !setor.isBlank()
        ) {
            return vagaService.buscarPorSetor(
                setor,
                paginacao
            );
        }

        if (tipo != null) {
            return vagaService.listarPorTipo(
                tipo,
                paginacao
            );
        }

        if (ativa != null) {
            return vagaService.listarPorStatus(
                ativa,
                paginacao
            );
        }

        return vagaService.listar(
            paginacao
        );
    }

    @GetMapping("/{id}")
    public VagaResponse buscarPorId(
        @PathVariable
        UUID id
    ) {
        return vagaService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public VagaResponse atualizar(
        @PathVariable
        UUID id,

        @Valid
        @RequestBody
        AtualizarVagaRequest request
    ) {
        return vagaService.atualizar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/status")
    public VagaResponse alterarStatus(
        @PathVariable
        UUID id,

        @Valid
        @RequestBody
        AlterarStatusVagaRequest request
    ) {
        return vagaService.alterarStatus(
            id,
            request
        );
    }
}