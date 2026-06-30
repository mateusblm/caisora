package br.com.caisora.ocupacao.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OcupacaoRepository {

    Ocupacao save(Ocupacao ocupacao);

    void flush();

    Optional<Ocupacao>
    findByIdAndOrganizacaoId(
        UUID id,
        UUID organizacaoId
    );

    Page<Ocupacao>
    findAllByOrganizacaoId(
        UUID organizacaoId,
        Pageable paginacao
    );

    Page<Ocupacao>
    findAllByOrganizacaoIdAndStatus(
        UUID organizacaoId,
        StatusOcupacao status,
        Pageable paginacao
    );

    Page<Ocupacao>
    findAllByOrganizacaoIdAndEmbarcacaoId(
        UUID organizacaoId,
        UUID embarcacaoId,
        Pageable paginacao
    );

    Page<Ocupacao>
    findAllByOrganizacaoIdAndVagaId(
        UUID organizacaoId,
        UUID vagaId,
        Pageable paginacao
    );

    Optional<Ocupacao>
    findByOrganizacaoIdAndEmbarcacaoIdAndStatus(
        UUID organizacaoId,
        UUID embarcacaoId,
        StatusOcupacao status
    );

    Optional<Ocupacao>
    findByOrganizacaoIdAndVagaIdAndStatus(
        UUID organizacaoId,
        UUID vagaId,
        StatusOcupacao status
    );

    boolean
    existsByOrganizacaoIdAndEmbarcacaoIdAndStatus(
        UUID organizacaoId,
        UUID embarcacaoId,
        StatusOcupacao status
    );

    boolean
    existsByOrganizacaoIdAndVagaIdAndStatus(
        UUID organizacaoId,
        UUID vagaId,
        StatusOcupacao status
    );
}
