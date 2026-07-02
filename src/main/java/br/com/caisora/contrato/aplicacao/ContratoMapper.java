package br.com.caisora.contrato.aplicacao;

import br.com.caisora.contrato.api.ContratoResponse;
import br.com.caisora.contrato.api.HistoricoContratoResponse;
import br.com.caisora.contrato.api.VinculoContratoOcupacaoResponse;
import br.com.caisora.contrato.dominio.Contrato;
import br.com.caisora.contrato.dominio.HistoricoContrato;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacao;
import org.springframework.stereotype.Component;

@Component
public class ContratoMapper {

    public ContratoResponse paraResponse(Contrato contrato) {
        return new ContratoResponse(
            contrato.getId(),
            contrato.getNumero(),
            contrato.getStatus(),
            contrato.getCliente().getId(),
            contrato.getCliente().getNome(),
            contrato.getEmbarcacao().getId(),
            contrato.getEmbarcacao().getNome(),
            contrato.getTipoVagaContratada(),
            contrato.getPeriodicidade(),
            contrato.getDataInicio(),
            contrato.getDataFim(),
            contrato.getDataAssinatura(),
            contrato.getDataAtivacao(),
            contrato.getDataEncerramento(),
            contrato.isRenovacaoAutomatica(),
            contrato.getDiasAvisoPrevio(),
            contrato.getValorBase(),
            contrato.getDiaVencimento(),
            contrato.getObservacoes(),
            contrato.getCriadoPor().getId(),
            contrato.getCriadoPor().getNome(),
            contrato.getOrganizacao().getId(),
            contrato.getCriadoEm(),
            contrato.getAtualizadoEm(),
            contrato.getVersao()
        );
    }

    public VinculoContratoOcupacaoResponse paraResponse(
        VinculoContratoOcupacao vinculo
    ) {
        return new VinculoContratoOcupacaoResponse(
            vinculo.getId(),
            vinculo.getContrato().getId(),
            vinculo.getOcupacao().getId(),
            vinculo.getOcupacao().getVaga().getId(),
            vinculo.getOcupacao().getVaga().getCodigo(),
            vinculo.getInicioEm(),
            vinculo.getFimEm(),
            vinculo.getMotivoFim(),
            vinculo.estaAberto()
        );
    }

    public HistoricoContratoResponse paraResponse(
        HistoricoContrato historico
    ) {
        return new HistoricoContratoResponse(
            historico.getId(),
            historico.getContrato().getId(),
            historico.getTipoEvento(),
            historico.getStatusAnterior(),
            historico.getStatusNovo(),
            historico.getDescricao(),
            historico.getRealizadoPor().getId(),
            historico.getRealizadoPor().getNome(),
            historico.getRealizadoEm()
        );
    }
}
