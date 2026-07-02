package br.com.caisora.contrato.infraestrutura;

import br.com.caisora.contrato.dominio.HistoricoContrato;
import br.com.caisora.contrato.dominio.HistoricoContratoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoContratoRepositoryJpa
    extends JpaRepository<HistoricoContrato, UUID>,
        HistoricoContratoRepository {
}
