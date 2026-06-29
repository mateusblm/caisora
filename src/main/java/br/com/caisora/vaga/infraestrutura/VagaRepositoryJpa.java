package br.com.caisora.vaga.infraestrutura;

import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VagaRepositoryJpa
    extends JpaRepository<Vaga, UUID>,
    VagaRepository {

}