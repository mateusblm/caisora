package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Periodo de aviso previo: nao da pra voltar atras, so concluir o encerramento. */
public class ContratoEmEncerramento implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.EM_ENCERRAMENTO; }

    @Override
    public StatusContrato encerrar() { return StatusContrato.ENCERRADO; }
}
