package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado concreto: permite apenas as operações declaradas abaixo. */
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
