package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/**
 * State: a entidade delega ao estado atual quais operações pode executar.
 * Cada método de transição devolve o próximo status; datas e auditoria ficam na entidade.
 */
public interface EstadoContrato {
    StatusContrato status();

    default void editar() { throw operacaoInvalida(); }
    default StatusContrato enviarParaAssinatura() { throw operacaoInvalida(); }
    default StatusContrato ativar() { throw operacaoInvalida(); }
    default StatusContrato suspender() { throw operacaoInvalida(); }
    default StatusContrato reativar() { throw operacaoInvalida(); }
    default StatusContrato encerrar() { throw operacaoInvalida(); }
    default boolean podeSerEditado() { return false; }
    default boolean podeReceberOcupacao() { return false; }

    default StatusContrato solicitarEncerramento() {
        throw new IllegalStateException("Somente contrato ativo ou suspenso pode entrar em encerramento");
    }

    default StatusContrato cancelar() {
        throw new IllegalStateException("Somente contrato em rascunho ou pendente de assinatura pode ser cancelado");
    }

    private IllegalStateException operacaoInvalida() {
        return new IllegalStateException("Operacao invalida para contrato no status " + status());
    }
}
