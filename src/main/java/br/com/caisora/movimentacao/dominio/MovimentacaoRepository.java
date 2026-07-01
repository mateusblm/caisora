package br.com.caisora.movimentacao.dominio;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimentacaoRepository {

    Movimentacao save(Movimentacao movimentacao);

    void flush();

    Optional<Movimentacao>
        findByIdAndOrganizacaoId(
            UUID id,
            UUID organizacaoId
        );

    Page<Movimentacao>
        findAllByOrganizacaoId(
            UUID organizacaoId,
            Pageable paginacao
        );

    Page<Movimentacao>
        findAllByOrganizacaoIdAndStatus(
            UUID organizacaoId,
            StatusMovimentacao status,
            Pageable paginacao
        );

    Page<Movimentacao>
        findAllByOrganizacaoIdAndStatusIn(
            UUID organizacaoId,
            Collection<StatusMovimentacao> status,
            Pageable paginacao
        );

    Page<Movimentacao>
        findAllByOrganizacaoIdAndTipo(
            UUID organizacaoId,
            TipoMovimentacao tipo,
            Pageable paginacao
        );

    Page<Movimentacao>
        findAllByOrganizacaoIdAndEmbarcacaoId(
            UUID organizacaoId,
            UUID embarcacaoId,
            Pageable paginacao
        );

    Page<Movimentacao>
        findAllByOrganizacaoIdAndAgendadaParaBetween(
            UUID organizacaoId,
            Instant inicio,
            Instant fim,
            Pageable paginacao
        );

    List<Movimentacao>
        findAllByOrganizacaoIdAndStatusInAndAgendadaParaBeforeOrderByAgendadaParaAsc(
            UUID organizacaoId,
            Collection<StatusMovimentacao> status,
            Instant limiteAgenda
        );

    List<Movimentacao>
        findAllByOrganizacaoIdAndStatusAndConcluidaEmBetweenOrderByConcluidaEmDesc(
            UUID organizacaoId,
            StatusMovimentacao status,
            Instant inicio,
            Instant fim
        );

    Optional<Movimentacao>
        findFirstByOrganizacaoIdAndEmbarcacaoIdAndStatusIn(
            UUID organizacaoId,
            UUID embarcacaoId,
            Collection<StatusMovimentacao> status
        );

    boolean
        existsByOrganizacaoIdAndEmbarcacaoIdAndStatusIn(
            UUID organizacaoId,
            UUID embarcacaoId,
            Collection<StatusMovimentacao> status
        );

    boolean
        existsByOrganizacaoIdAndVagaDestinoIdAndStatusIn(
            UUID organizacaoId,
            UUID vagaDestinoId,
            Collection<StatusMovimentacao> status
        );

    boolean
        existsByOrganizacaoIdAndVagaDestinoIdAndStatusInAndIdNot(
            UUID organizacaoId,
            UUID vagaDestinoId,
            Collection<StatusMovimentacao> status,
            UUID id
        );
}
