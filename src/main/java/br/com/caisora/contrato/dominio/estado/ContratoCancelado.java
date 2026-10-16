package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado final de quem nunca chegou a ser ativado. Tambem nao aceita nenhuma operacao. */
public class ContratoCancelado implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.CANCELADO; }
}
