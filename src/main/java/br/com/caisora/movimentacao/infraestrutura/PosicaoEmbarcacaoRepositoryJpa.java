package br.com.caisora.movimentacao.infraestrutura;

import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacaoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosicaoEmbarcacaoRepositoryJpa
    extends
        JpaRepository<
            PosicaoEmbarcacao,
            UUID
        >,
        PosicaoEmbarcacaoRepository {
}
