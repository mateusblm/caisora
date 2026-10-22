package br.com.caisora.variabilidade.api;

import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.variabilidade.aplicacao.VariabilidadeService;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizacoes/{organizacaoId}/modulos")
@PreAuthorize("hasRole('ADMINISTRADOR_PLATAFORMA')")
public class ModulosOrganizacaoController {
    private final VariabilidadeService service;
    private final OrganizacaoService organizacoes;
    public ModulosOrganizacaoController(VariabilidadeService service, OrganizacaoService organizacoes) {
        this.service=service;
        this.organizacoes=organizacoes;
    }
    @GetMapping public ConfiguracaoVariabilidadeResponse consultar(@PathVariable UUID organizacaoId) {
        organizacoes.buscarPorId(organizacaoId);
        return service.obterConfiguracao(organizacaoId);
    }
    @PutMapping("/{modulo}") public ConfiguracaoVariabilidadeResponse atualizar(@PathVariable UUID organizacaoId,
    @PathVariable ModuloSistema modulo, @Valid @RequestBody AtualizarModuloRequest request) {
        organizacoes.buscarPorId(organizacaoId);
        return service.atualizarModulo(organizacaoId,modulo,request.ativo());
    }
}
