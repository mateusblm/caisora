package br.com.caisora.movimentacao.api;

import br.com.caisora.movimentacao.aplicacao.MovimentacaoService;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
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
@RequestMapping("/api/v1/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService
        movimentacaoService;

    public MovimentacaoController(
        MovimentacaoService
            movimentacaoService
    ) {
        this.movimentacaoService =
            movimentacaoService;
    }

    @PostMapping
    public ResponseEntity<MovimentacaoResponse>
    criar(
        @Valid
        @RequestBody
        CriarMovimentacaoRequest request
    ) {
        MovimentacaoResponse movimentacao =
            movimentacaoService.criar(request);

        URI localizacao = URI.create(
            "/api/v1/movimentacoes/"
                + movimentacao.id()
        );

        return ResponseEntity
            .created(localizacao)
            .body(movimentacao);
    }

    @GetMapping
    public Page<MovimentacaoResponse> listar(
        @RequestParam(required = false)
        StatusMovimentacao status,

        @RequestParam(required = false)
        TipoMovimentacao tipo,

        @RequestParam(required = false)
        UUID embarcacaoId,

        @RequestParam(required = false)
        Instant inicio,

        @RequestParam(required = false)
        Instant fim,

        @PageableDefault(
            size = 20,
            sort = "agendadaPara",
            direction = Sort.Direction.ASC
        )
        Pageable paginacao
    ) {
        return movimentacaoService.listar(
            status,
            tipo,
            embarcacaoId,
            inicio,
            fim,
            paginacao
        );
    }

    @GetMapping("/{id}")
    public MovimentacaoResponse buscarPorId(
        @PathVariable UUID id
    ) {
        return movimentacaoService
            .buscarPorId(id);
    }

    @PutMapping("/{id}")
    public MovimentacaoResponse atualizar(
        @PathVariable UUID id,

        @Valid
        @RequestBody
        AtualizarMovimentacaoRequest request
    ) {
        return movimentacaoService.atualizar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/inicio")
    public MovimentacaoResponse iniciar(
        @PathVariable UUID id,

        @Valid
        @RequestBody
        IniciarMovimentacaoRequest request
    ) {
        return movimentacaoService.iniciar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/conclusao")
    public MovimentacaoResponse concluir(
        @PathVariable UUID id,

        @Valid
        @RequestBody
        ConcluirMovimentacaoRequest request
    ) {
        return movimentacaoService.concluir(
            id,
            request
        );
    }

    @PatchMapping("/{id}/cancelamento")
    public MovimentacaoResponse cancelar(
        @PathVariable UUID id,

        @Valid
        @RequestBody
        CancelarMovimentacaoRequest request
    ) {
        return movimentacaoService.cancelar(
            id,
            request
        );
    }

    @GetMapping("/{id}/historico")
    public Page<HistoricoMovimentacaoResponse>
    listarHistorico(
        @PathVariable UUID id,

        @PageableDefault(
            size = 20,
            sort = "ocorridoEm",
            direction = Sort.Direction.DESC
        )
        Pageable paginacao
    ) {
        return movimentacaoService
            .listarHistorico(
                id,
                paginacao
            );
    }
}
