package br.com.caisora.contrato.infraestrutura;

import br.com.caisora.contrato.dominio.VinculoContratoOcupacao;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacaoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoContratoOcupacaoRepositoryJpa
    extends JpaRepository<VinculoContratoOcupacao, UUID>,
        VinculoContratoOcupacaoRepository {
}
