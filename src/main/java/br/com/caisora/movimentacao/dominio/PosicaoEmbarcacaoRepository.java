package br.com.caisora.movimentacao.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosicaoEmbarcacaoRepository {

    PosicaoEmbarcacao save(
        PosicaoEmbarcacao posicao
    );

    Optional<PosicaoEmbarcacao>
    findByIdAndOrganizacaoId(
        UUID id,
        UUID organizacaoId
    );

    Optional<PosicaoEmbarcacao>
    findByOrganizacaoIdAndEmbarcacaoId(
        UUID organizacaoId,
        UUID embarcacaoId
    );

    Page<PosicaoEmbarcacao>
    findAllByOrganizacaoId(
        UUID organizacaoId,
        Pageable paginacao
    );

    Page<PosicaoEmbarcacao>
    findAllByOrganizacaoIdAndTipo(
        UUID organizacaoId,
        TipoPosicaoEmbarcacao tipo,
        Pageable paginacao
    );

    Page<PosicaoEmbarcacao>
    findAllByOrganizacaoIdAndVagaId(
        UUID organizacaoId,
        UUID vagaId,
        Pageable paginacao
    );

    boolean
    existsByOrganizacaoIdAndEmbarcacaoId(
        UUID organizacaoId,
        UUID embarcacaoId
    );
}
