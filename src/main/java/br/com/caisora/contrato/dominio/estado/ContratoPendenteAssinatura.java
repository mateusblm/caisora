package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado concreto: permite apenas as operações declaradas abaixo. */
public class ContratoPendenteAssinatura implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.PENDENTE_ASSINATURA; }

    @Override
    public StatusContrato ativar() { return StatusContrato.ATIVO; }

    @Override
    public StatusContrato cancelar() { return StatusContrato.CANCELADO; }
}
