package br.com.caisora.movimentacao.dominio;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoricoMovimentacaoRepository {

    HistoricoMovimentacao save(
        HistoricoMovimentacao historico
    );

    Page<HistoricoMovimentacao>
    findAllByOrganizacaoIdAndMovimentacaoId(
        UUID organizacaoId,
        UUID movimentacaoId,
        Pageable paginacao
    );
}
