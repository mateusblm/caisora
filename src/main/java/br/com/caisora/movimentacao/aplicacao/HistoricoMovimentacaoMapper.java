package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.movimentacao.api.HistoricoMovimentacaoResponse;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacao;
import org.springframework.stereotype.Component;

@Component
public class HistoricoMovimentacaoMapper {

    public HistoricoMovimentacaoResponse
    paraResponse(
        HistoricoMovimentacao historico
    ) {
        return new HistoricoMovimentacaoResponse(
            historico.getId(),
            historico
                .getMovimentacao()
                .getId(),

            historico.getTipoEvento(),
            historico.getStatusAnterior(),
            historico.getStatusNovo(),

            historico
                .getAgendadaParaAnterior(),
            historico
                .getAgendadaParaNova(),

            historico
                .getUsuario()
                .getId(),
            historico
                .getUsuario()
                .getNome(),

            historico.getObservacao(),
            historico.getDadosAnteriores(),
            historico.getDadosNovos(),

            historico
                .getOrganizacao()
                .getId(),
            historico.getOcorridoEm()
        );
    }
}
