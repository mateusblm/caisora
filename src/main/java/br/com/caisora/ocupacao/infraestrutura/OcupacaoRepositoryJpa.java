package br.com.caisora.ocupacao.infraestrutura;

import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OcupacaoRepositoryJpa
    extends JpaRepository<Ocupacao, UUID>,
    OcupacaoRepository {

}