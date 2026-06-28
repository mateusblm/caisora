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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    public static final String HEADER_ORGANIZACAO_ID = "X-Organizacao-Id";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * MVP temporario: recebe a organizacao por header para permitir testes no Postman.
     * No fluxo definitivo, a organizacao vira parte do contexto autenticado via JWT ou
     * subdominio, evitando confiar em dado livre vindo do frontend.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            @Valid @RequestBody CriarUsuarioRequest request
    ) {
        return usuarioService.criar(organizacaoId, request);
    }

    @GetMapping
    public Page<UsuarioResponse> listar(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            Pageable paginacao
    ) {
        return usuarioService.listar(organizacaoId, paginacao);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            @PathVariable UUID id
    ) {
        return usuarioService.buscarPorId(organizacaoId, id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        return usuarioService.atualizar(organizacaoId, id, request);
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(
            @RequestHeader(HEADER_ORGANIZACAO_ID) UUID organizacaoId,
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusUsuarioRequest request
    ) {
        return usuarioService.alterarStatus(organizacaoId, id, request);
    }
}
