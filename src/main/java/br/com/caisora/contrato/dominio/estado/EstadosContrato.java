package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Reconstrói o comportamento a partir do enum persistido, inclusive após leitura pelo JPA. */
public final class EstadosContrato {
    private EstadosContrato() { }

    public static EstadoContrato para(StatusContrato status) {
        return switch (status) {
            case RASCUNHO -> new ContratoRascunho();
            case PENDENTE_ASSINATURA -> new ContratoPendenteAssinatura();
            case ATIVO -> new ContratoAtivo();
            case SUSPENSO -> new ContratoSuspenso();
            case EM_ENCERRAMENTO -> new ContratoEmEncerramento();
            case ENCERRADO -> new ContratoEncerrado();
            case CANCELADO -> new ContratoCancelado();
        };
    }
}
