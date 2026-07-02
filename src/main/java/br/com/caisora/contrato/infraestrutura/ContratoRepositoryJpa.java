package br.com.caisora.contrato.infraestrutura;

import br.com.caisora.contrato.dominio.Contrato;
import br.com.caisora.contrato.dominio.ContratoRepository;
import br.com.caisora.contrato.dominio.StatusContrato;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoRepositoryJpa
    extends JpaRepository<Contrato, UUID>, ContratoRepository {

    @Override
    @Query(
        """
        select c
          from Contrato c
         where c.organizacao.id = :organizacaoId
           and (:status is null or c.status = :status)
           and (:clienteId is null or c.cliente.id = :clienteId)
           and (:embarcacaoId is null or c.embarcacao.id = :embarcacaoId)
        """
    )
    Page<Contrato> buscarPorFiltros(
        @Param("organizacaoId") UUID organizacaoId,
        @Param("status") StatusContrato status,
        @Param("clienteId") UUID clienteId,
        @Param("embarcacaoId") UUID embarcacaoId,
        Pageable paginacao
    );
}
