package br.com.caisora.contrato.dominio;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContratoRepository {

    Contrato save(Contrato contrato);

    Optional<Contrato> findByIdAndOrganizacaoId(
        UUID id,
        UUID organizacaoId
    );

    Page<Contrato> buscarPorFiltros(
        UUID organizacaoId,
        StatusContrato status,
        UUID clienteId,
        UUID embarcacaoId,
        Pageable paginacao
    );

    boolean existsByOrganizacaoIdAndEmbarcacaoIdAndStatusInAndIdNot(
        UUID organizacaoId,
        UUID embarcacaoId,
        Collection<StatusContrato> status,
        UUID id
    );
}
