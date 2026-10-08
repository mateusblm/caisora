package br.com.caisora.contrato.dominio.estado;

import br.com.caisora.contrato.dominio.StatusContrato;

/** Estado concreto: permite apenas as operações declaradas abaixo. */
public class ContratoRascunho implements EstadoContrato {
    @Override
    public StatusContrato status() { return StatusContrato.RASCUNHO; }

    @Override
    public StatusContrato enviarParaAssinatura() { return StatusContrato.PENDENTE_ASSINATURA; }

    @Override
    public StatusContrato cancelar() { return StatusContrato.CANCELADO; }

    @Override
    public void editar() { /* Rascunhos permitem edição. */ }

    @Override
    public boolean podeSerEditado() { return true; }
}
