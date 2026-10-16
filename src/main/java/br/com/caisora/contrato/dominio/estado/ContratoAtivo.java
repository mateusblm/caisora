package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Contrato em vigor. E o unico estado em que a embarcacao pode ocupar uma vaga. */
public class ContratoAtivo implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.ATIVO; }

    @Override
    public StatusContrato suspender() { return StatusContrato.SUSPENSO; }

    @Override
    public StatusContrato solicitarEncerramento() { return StatusContrato.EM_ENCERRAMENTO; }

    @Override
    public boolean podeReceberOcupacao() { return true; }
}
