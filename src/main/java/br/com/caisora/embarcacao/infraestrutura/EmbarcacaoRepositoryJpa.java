package br.com.caisora.embarcacao.infraestrutura;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmbarcacaoRepositoryJpa
        extends JpaRepository<Embarcacao, UUID>,
        EmbarcacaoRepository {
}