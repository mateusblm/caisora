package br.com.caisora.movimentacao.infraestrutura;

import br.com.caisora.movimentacao.dominio.HistoricoMovimentacao;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacaoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoMovimentacaoRepositoryJpa
    extends
        JpaRepository<
            HistoricoMovimentacao,
            UUID
        >,
        HistoricoMovimentacaoRepository {
}
