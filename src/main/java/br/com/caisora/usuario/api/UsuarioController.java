package br.com.caisora.usuario.api;

import br.com.caisora.usuario.aplicacao.UsuarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return usuarioService.criar(request);
    }

    @GetMapping
    public Page<UsuarioResponse> listar(Pageable paginacao) {
        return usuarioService.listar(paginacao);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable UUID id) {
        return usuarioService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        return usuarioService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusUsuarioRequest request
    ) {
        return usuarioService.alterarStatus(id, request);
    }
}
