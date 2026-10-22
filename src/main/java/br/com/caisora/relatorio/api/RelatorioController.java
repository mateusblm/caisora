package br.com.caisora.relatorio.api;

import br.com.caisora.relatorio.aplicacao.RelatorioService;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.contrato.dominio.*;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relatorios")
@PreAuthorize("@acesso.permitido(authentication, 'RELATORIOS', 'VISUALIZAR')")
public class RelatorioController {
    private final RelatorioService service;
    private final LeitorTokenJwt leitor;
    public RelatorioController(RelatorioService service,LeitorTokenJwt leitor) {
        this.service=service;
        this.leitor=leitor;
    }
    @GetMapping("/ocupacao")
    @PreAuthorize("@acesso.permitido(authentication, 'RELATORIOS', 'VISUALIZAR') and @acesso.permitido(authentication, 'VAGAS', 'VISUALIZAR') and @acesso.permitido(authentication, 'OCUPACOES', 'VISUALIZAR')")
    public RelatoriosResponse.Ocupacao ocupacao(@RequestParam(required=false) String setor,@RequestParam(required=false) TipoVaga tipo) {
        return service.ocupacao(leitor.obterUsuarioAutenticado().organizacaoId(),setor,tipo);
    }
    @GetMapping("/movimentacoes")
    @PreAuthorize("@acesso.permitido(authentication, 'RELATORIOS', 'VISUALIZAR') and @acesso.permitido(authentication, 'MOVIMENTACOES', 'VISUALIZAR')")
    public RelatoriosResponse.Movimentacoes movimentacoes(@RequestParam LocalDate inicio,@RequestParam LocalDate fim,@RequestParam(required=false) TipoMovimentacao tipo) {
        return service.movimentacoes(leitor.obterUsuarioAutenticado().organizacaoId(),inicio,fim,tipo);
    }
    @GetMapping("/contratos")
    @PreAuthorize("@acesso.permitido(authentication, 'RELATORIOS', 'VISUALIZAR') and @acesso.permitido(authentication, 'CONTRATOS', 'VISUALIZAR')")
    public RelatoriosResponse.Contratos contratos(@RequestParam(required=false) StatusContrato status,@RequestParam(required=false) PeriodicidadeContrato periodicidade,
    @RequestParam(required=false) LocalDate inicio,@RequestParam(required=false) LocalDate fim) {
        return service.contratos(leitor.obterUsuarioAutenticado().organizacaoId(),status,periodicidade,inicio,fim);
    }
}
