package br.com.caisora.movimentacao.infraestrutura;

import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepositoryJpa
    extends
        JpaRepository<Movimentacao, UUID>,
        MovimentacaoRepository {
}
