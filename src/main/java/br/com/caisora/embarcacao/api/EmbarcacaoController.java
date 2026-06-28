package br.com.caisora.embarcacao.api;

import br.com.caisora.embarcacao.aplicacao.EmbarcacaoService;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;

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

@RestController
@RequestMapping("/api/v1/embarcacoes")
public class EmbarcacaoController {

    private final EmbarcacaoService embarcacaoService;

    public EmbarcacaoController(
            EmbarcacaoService embarcacaoService
    ) {
        this.embarcacaoService = embarcacaoService;
    }

    @PostMapping
    public ResponseEntity<EmbarcacaoResponse> criar(
            @Valid
            @RequestBody
            CriarEmbarcacaoRequest request
    ) {
        EmbarcacaoResponse embarcacao =
                embarcacaoService.criar(request);

        URI localizacao = URI.create(
                "/api/v1/embarcacoes/"
                        + embarcacao.id()
        );

        return ResponseEntity
                .created(localizacao)
                .body(embarcacao);
    }

    @GetMapping
    public Page<EmbarcacaoResponse> listar(
            @RequestParam(required = false)
            String nome,

            @RequestParam(required = false)
            Boolean ativa,

            @RequestParam(required = false)
            UUID proprietarioId,

            @RequestParam(required = false)
            TipoEmbarcacao tipo,

            @PageableDefault(
                    size = 20,
                    sort = "nome",
                    direction = Sort.Direction.ASC
            )
            Pageable paginacao
    ) {
        if (nome != null && !nome.isBlank()) {
            return embarcacaoService.buscarPorNome(
                    nome,
                    paginacao
            );
        }

        if (proprietarioId != null) {
            return embarcacaoService.listarPorProprietario(
                    proprietarioId,
                    paginacao
            );
        }

        if (tipo != null) {
            return embarcacaoService.listarPorTipo(
                    tipo,
                    paginacao
            );
        }

        if (ativa != null) {
            return embarcacaoService.listarPorStatus(
                    ativa,
                    paginacao
            );
        }

        return embarcacaoService.listar(paginacao);
    }

    @GetMapping("/{id}")
    public EmbarcacaoResponse buscarPorId(
            @PathVariable UUID id
    ) {
        return embarcacaoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public EmbarcacaoResponse atualizar(
            @PathVariable UUID id,
            @Valid
            @RequestBody
            AtualizarEmbarcacaoRequest request
    ) {
        return embarcacaoService.atualizar(
                id,
                request
        );
    }

    @PatchMapping("/{id}/status")
    public EmbarcacaoResponse alterarStatus(
            @PathVariable UUID id,
            @Valid
            @RequestBody
            AlterarStatusEmbarcacaoRequest request
    ) {
        return embarcacaoService.alterarStatus(
                id,
                request
        );
    }
}