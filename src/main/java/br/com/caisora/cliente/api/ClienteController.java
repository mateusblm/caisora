package br.com.caisora.cliente.api;

import br.com.caisora.cliente.aplicacao.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(
        @Valid @RequestBody CriarClienteRequest request
    ) {
        ClienteResponse cliente = clienteService.criar(request);

        URI localizacao =
            URI.create("/api/v1/clientes/" + cliente.id());

        return ResponseEntity
            .created(localizacao)
            .body(cliente);
    }

    @GetMapping
    public Page<ClienteResponse> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) Boolean ativo,
        @PageableDefault(
            size = 20,
            sort = "nome",
            direction = Sort.Direction.ASC
        )
        Pageable paginacao
    ) {
        if (nome != null && !nome.isBlank()) {
            return clienteService.buscarPorNome(
                nome,
                paginacao
            );
        }

        if (ativo != null) {
            return clienteService.listarPorStatus(
                ativo,
                paginacao
            );
        }

        return clienteService.listar(paginacao);
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(
        @PathVariable UUID id
    ) {
        return clienteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AtualizarClienteRequest request
    ) {
        return clienteService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public ClienteResponse alterarStatus(
        @PathVariable UUID id,
        @Valid
        @RequestBody AlterarStatusClienteRequest request
    ) {
        return clienteService.alterarStatus(id, request);
    }
}