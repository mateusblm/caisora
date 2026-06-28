package br.com.caisora.organizacao.api;

import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizacoes")
@PreAuthorize("principal.claims['perfil'] == 'ADMINISTRADOR_PLATAFORMA'")
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    public OrganizacaoController(OrganizacaoService organizacaoService) {
        this.organizacaoService = organizacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(@Valid @RequestBody CriarOrganizacaoRequest request) {
        return organizacaoService.criar(request);
    }

    @GetMapping
    public Page<OrganizacaoResponse> listar(Pageable paginacao) {
        return organizacaoService.listar(paginacao);
    }

    @GetMapping("/{id}")
    public OrganizacaoResponse buscarPorId(@PathVariable UUID id) {
        return organizacaoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public OrganizacaoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarOrganizacaoRequest request
    ) {
        return organizacaoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        organizacaoService.inativar(id);
    }
}
