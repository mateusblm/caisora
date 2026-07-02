package br.com.caisora.contrato.dominio;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoricoContratoRepository {

    HistoricoContrato save(HistoricoContrato historico);

    Page<HistoricoContrato>
        findAllByContratoIdAndOrganizacaoIdOrderByRealizadoEmDesc(
            UUID contratoId,
            UUID organizacaoId,
            Pageable paginacao
        );
}
