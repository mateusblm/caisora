package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado final. Nao sobrescreve nada, entao qualquer operacao cai no erro padrao da interface. */
public class ContratoEncerrado implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.ENCERRADO; }
}
