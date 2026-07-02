package br.com.caisora.contrato.api;

import br.com.caisora.contrato.aplicacao.ContratoService;
import br.com.caisora.contrato.dominio.StatusContrato;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> criar(
        @Valid @RequestBody CriarContratoRequest request
    ) {
        ContratoResponse contrato = contratoService.criar(request);
        URI localizacao = URI.create(
            "/api/v1/contratos/" + contrato.id()
        );
        return ResponseEntity.created(localizacao).body(contrato);
    }

    @GetMapping
    public Page<ContratoResponse> listar(
        @RequestParam(required = false) StatusContrato status,
        @RequestParam(required = false) UUID clienteId,
        @RequestParam(required = false) UUID embarcacaoId,
        @PageableDefault(
            size = 20,
            sort = "criadoEm",
            direction = Sort.Direction.DESC
        ) Pageable paginacao
    ) {
        return contratoService.listar(
            status,
            clienteId,
            embarcacaoId,
            paginacao
        );
    }

    @GetMapping("/{id}")
    public ContratoResponse buscarPorId(@PathVariable UUID id) {
        return contratoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ContratoResponse atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AtualizarContratoRequest request
    ) {
        return contratoService.atualizar(id, request);
    }

    @PostMapping("/{id}/enviar-para-assinatura")
    public ContratoResponse enviarParaAssinatura(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) MotivoContratoRequest request
    ) {
        return contratoService.enviarParaAssinatura(
            id,
            request == null ? null : request.descricao()
        );
    }

    @PostMapping("/{id}/ativar")
    public ContratoResponse ativar(
        @PathVariable UUID id,
        @Valid @RequestBody AtivarContratoRequest request
    ) {
        return contratoService.ativar(id, request);
    }

    @PostMapping("/{id}/suspender")
    public ContratoResponse suspender(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) MotivoContratoRequest request
    ) {
        return contratoService.suspender(
            id,
            request == null ? null : request.descricao()
        );
    }

    @PostMapping("/{id}/reativar")
    public ContratoResponse reativar(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) MotivoContratoRequest request
    ) {
        return contratoService.reativar(
            id,
            request == null ? null : request.descricao()
        );
    }

    @PostMapping("/{id}/solicitar-encerramento")
    public ContratoResponse solicitarEncerramento(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) MotivoContratoRequest request
    ) {
        return contratoService.solicitarEncerramento(
            id,
            request == null ? null : request.descricao()
        );
    }

    @PostMapping("/{id}/encerrar")
    public ContratoResponse encerrar(
        @PathVariable UUID id,
        @Valid @RequestBody EncerrarContratoRequest request
    ) {
        return contratoService.encerrar(id, request);
    }

    @PostMapping("/{id}/cancelar")
    public ContratoResponse cancelar(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) MotivoContratoRequest request
    ) {
        return contratoService.cancelar(
            id,
            request == null ? null : request.descricao()
        );
    }

    @PostMapping("/{id}/ocupacoes")
    public ResponseEntity<VinculoContratoOcupacaoResponse> vincularOcupacao(
        @PathVariable UUID id,
        @Valid @RequestBody VincularOcupacaoContratoRequest request
    ) {
        VinculoContratoOcupacaoResponse vinculo =
            contratoService.vincularOcupacao(id, request);
        URI localizacao = URI.create(
            "/api/v1/contratos/" + id + "/ocupacoes/" + vinculo.id()
        );
        return ResponseEntity.created(localizacao).body(vinculo);
    }

    @GetMapping("/{id}/ocupacoes")
    public List<VinculoContratoOcupacaoResponse> listarOcupacoes(
        @PathVariable UUID id
    ) {
        return contratoService.listarOcupacoes(id);
    }

    @DeleteMapping("/{id}/ocupacoes/{vinculoId}")
    public VinculoContratoOcupacaoResponse desvincularOcupacao(
        @PathVariable UUID id,
        @PathVariable UUID vinculoId,
        @RequestParam(required = false) String motivo
    ) {
        return contratoService.desvincularOcupacao(
            id,
            vinculoId,
            motivo
        );
    }

    @GetMapping("/{id}/historico")
    public Page<HistoricoContratoResponse> listarHistorico(
        @PathVariable UUID id,
        @PageableDefault(
            size = 20,
            sort = "realizadoEm",
            direction = Sort.Direction.DESC
        ) Pageable paginacao
    ) {
        return contratoService.listarHistorico(id, paginacao);
    }
}
