package br.com.caisora.vaga.dominio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface VagaRepository {

    Vaga save(Vaga vaga);

    Optional<Vaga> findByIdAndOrganizacaoId(
        UUID id,
        UUID organizacaoId
    );

    Page<Vaga> findAllByOrganizacaoId(
        UUID organizacaoId,
        Pageable paginacao
    );

    Page<Vaga> findAllByOrganizacaoIdAndAtiva(
        UUID organizacaoId,
        boolean ativa,
        Pageable paginacao
    );

    Page<Vaga>
    findAllByOrganizacaoIdAndCodigoContainingIgnoreCase(
        UUID organizacaoId,
        String codigo,
        Pageable paginacao
    );

    Page<Vaga> findAllByOrganizacaoIdAndTipo(
        UUID organizacaoId,
        TipoVaga tipo,
        Pageable paginacao
    );

    Page<Vaga>
    findAllByOrganizacaoIdAndSetorContainingIgnoreCase(
        UUID organizacaoId,
        String setor,
        Pageable paginacao
    );

    boolean existsByOrganizacaoIdAndCodigoIgnoreCase(
        UUID organizacaoId,
        String codigo
    );

    boolean
    existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
        UUID organizacaoId,
        String codigo,
        UUID id
    );
}