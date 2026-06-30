package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.vaga.dominio.Vaga;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoMapper {

    public MovimentacaoResponse paraResponse(
        Movimentacao movimentacao
    ) {
        Embarcacao embarcacao =
            movimentacao.getEmbarcacao();

        Vaga vagaOrigem =
            movimentacao.getVagaOrigem();

        Vaga vagaDestino =
            movimentacao.getVagaDestino();

        Usuario solicitante =
            movimentacao.getSolicitadaPor();

        Usuario operador =
            movimentacao
                .getOperadorResponsavel();

        return new MovimentacaoResponse(
            movimentacao.getId(),

            embarcacao.getId(),
            obterNomeEmbarcacao(
                embarcacao
            ),
            embarcacao.getModelo(),
            embarcacao
                .getProprietario()
                .getNome(),

            movimentacao.getTipo(),
            movimentacao.getStatus(),
            movimentacao.getPrioridade(),

            movimentacao
                .getTipoPosicaoOrigem(),

            obterId(vagaOrigem),
            obterCodigo(vagaOrigem),
            movimentacao
                .getDescricaoOrigem(),

            movimentacao
                .getTipoPosicaoDestino(),

            obterId(vagaDestino),
            obterCodigo(vagaDestino),
            movimentacao
                .getDescricaoDestino(),

            movimentacao.getAgendadaPara(),
            movimentacao.getIniciadaEm(),
            movimentacao.getConcluidaEm(),
            movimentacao.getCanceladaEm(),

            solicitante.getId(),
            solicitante.getNome(),

            operador == null
                ? null
                : operador.getId(),

            operador == null
                ? null
                : operador.getNome(),

            movimentacao.getObservacoes(),
            movimentacao
                .getMotivoCancelamento(),

            movimentacao.getVersao(),
            movimentacao
                .getOrganizacao()
                .getId(),
            movimentacao.getCriadaEm(),
            movimentacao.getAtualizadaEm()
        );
    }

    private String obterNomeEmbarcacao(
        Embarcacao embarcacao
    ) {
        if (
            embarcacao.getNome() != null
            && !embarcacao.getNome()
                .isBlank()
        ) {
            return embarcacao.getNome();
        }

        if (
            embarcacao.getModelo() != null
            && !embarcacao.getModelo()
                .isBlank()
        ) {
            return embarcacao.getModelo();
        }

        if (
            embarcacao
                .getNumeroInscricao()
                != null
            && !embarcacao
                .getNumeroInscricao()
                .isBlank()
        ) {
            return embarcacao
                .getNumeroInscricao();
        }

        return "Embarcação sem nome";
    }

    private java.util.UUID obterId(
        Vaga vaga
    ) {
        return vaga == null
            ? null
            : vaga.getId();
    }

    private String obterCodigo(
        Vaga vaga
    ) {
        return vaga == null
            ? null
            : vaga.getCodigo();
    }
}
