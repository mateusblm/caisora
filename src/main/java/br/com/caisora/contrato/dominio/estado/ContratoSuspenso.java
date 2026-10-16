package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Contrato pausado. Pode voltar a ser ativo ou ja ir pro encerramento. */
public class ContratoSuspenso implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.SUSPENSO; }

    @Override
    public StatusContrato reativar() { return StatusContrato.ATIVO; }

    @Override
    public StatusContrato solicitarEncerramento() { return StatusContrato.EM_ENCERRAMENTO; }
}
