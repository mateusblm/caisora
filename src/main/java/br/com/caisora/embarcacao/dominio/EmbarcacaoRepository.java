package br.com.caisora.embarcacao.dominio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EmbarcacaoRepository {

    Embarcacao save(Embarcacao embarcacao);

    Optional<Embarcacao> findByIdAndOrganizacaoId(
            UUID id,
            UUID organizacaoId
    );

    Page<Embarcacao> findAllByOrganizacaoId(
            UUID organizacaoId,
            Pageable paginacao
    );

    Page<Embarcacao> findAllByOrganizacaoIdAndAtiva(
            UUID organizacaoId,
            boolean ativa,
            Pageable paginacao
    );

    Page<Embarcacao>
    findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
            UUID organizacaoId,
            String nome,
            Pageable paginacao
    );

    Page<Embarcacao> findAllByOrganizacaoIdAndProprietarioId(
            UUID organizacaoId,
            UUID proprietarioId,
            Pageable paginacao
    );

    Page<Embarcacao> findAllByOrganizacaoIdAndTipo(
            UUID organizacaoId,
            TipoEmbarcacao tipo,
            Pageable paginacao
    );

    boolean existsByOrganizacaoIdAndNumeroInscricaoIgnoreCase(
            UUID organizacaoId,
            String numeroInscricao
    );

    boolean
    existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
            UUID organizacaoId,
            String numeroInscricao,
            UUID id
    );

    boolean existsByOrganizacaoIdAndNumeroCascoIgnoreCase(
            UUID organizacaoId,
            String numeroCasco
    );

    boolean
    existsByOrganizacaoIdAndNumeroCascoIgnoreCaseAndIdNot(
            UUID organizacaoId,
            String numeroCasco,
            UUID id
    );
}