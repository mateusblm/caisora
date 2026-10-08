package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado concreto: permite apenas as operações declaradas abaixo. */
public class ContratoEmEncerramento implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.EM_ENCERRAMENTO; }

    @Override
    public StatusContrato encerrar() { return StatusContrato.ENCERRADO; }
}
