package br.com.caisora.referencia.api;

import br.com.caisora.referencia.aplicacao.ReferenciasService;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.referencia.api.ReferenciasResponse.*;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/referencias")
@PreAuthorize("@acesso.permitido(authentication, 'REFERENCIAS', 'VISUALIZAR')")
public class ReferenciasController {
    private final ReferenciasService service;
    private final LeitorTokenJwt leitor;
    public ReferenciasController(ReferenciasService service, LeitorTokenJwt leitor) { this.service=service; this.leitor=leitor; }
    @GetMapping("/paises") public Consulta<Pais> paises(@RequestParam(required=false) String busca) { return service.paises(busca); }
    @GetMapping("/municipios") public Consulta<Municipio> municipios(@RequestParam String uf,@RequestParam(required=false) String busca) { return service.municipios(uf,busca); }
    @GetMapping("/feriados") public Consulta<Feriado> feriados(@RequestParam int ano,@RequestParam(required=false) String uf) { return service.feriados(ano,uf); }
    @GetMapping("/mares/disponibilidade") public DisponibilidadeMares disponibilidade() { return service.disponibilidadeMares(); }
    @GetMapping("/mares") public Mares mares(@RequestParam LocalDate inicio,@RequestParam(defaultValue="1") int dias) {
        return service.mares(leitor.obterUsuarioAutenticado().organizacaoId(),inicio,dias);
    }
}
