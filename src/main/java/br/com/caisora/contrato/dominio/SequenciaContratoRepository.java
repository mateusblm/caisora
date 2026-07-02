package br.com.caisora.contrato.dominio;

import java.util.UUID;

public interface SequenciaContratoRepository {

    long proximoNumero(UUID organizacaoId, int ano);
}
