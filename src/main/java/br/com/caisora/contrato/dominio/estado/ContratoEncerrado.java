package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado concreto: permite apenas as operações declaradas abaixo. */
public class ContratoEncerrado implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.ENCERRADO; }
}
