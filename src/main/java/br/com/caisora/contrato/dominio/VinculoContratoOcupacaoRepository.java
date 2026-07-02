package br.com.caisora.contrato.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VinculoContratoOcupacaoRepository {

    VinculoContratoOcupacao save(VinculoContratoOcupacao vinculo);

    Optional<VinculoContratoOcupacao> findByIdAndContratoIdAndOrganizacaoId(
        UUID id,
        UUID contratoId,
        UUID organizacaoId
    );

    Optional<VinculoContratoOcupacao>
        findFirstByContratoIdAndOrganizacaoIdAndFimEmIsNull(
            UUID contratoId,
            UUID organizacaoId
        );

    Optional<VinculoContratoOcupacao>
        findFirstByOcupacaoIdAndOrganizacaoIdAndFimEmIsNull(
            UUID ocupacaoId,
            UUID organizacaoId
        );

    List<VinculoContratoOcupacao>
        findAllByContratoIdAndOrganizacaoIdOrderByInicioEmDesc(
            UUID contratoId,
            UUID organizacaoId
        );
}
