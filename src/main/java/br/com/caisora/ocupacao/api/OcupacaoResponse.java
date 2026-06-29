package br.com.caisora.ocupacao.api;

import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.vaga.dominio.TipoVaga;

import java.time.Instant;
import java.util.UUID;

public record OcupacaoResponse(

    UUID id,

    UUID embarcacaoId,

    String embarcacaoNome,

    String embarcacaoModelo,

    String proprietarioNome,

    UUID vagaId,

    String vagaCodigo,

    TipoVaga vagaTipo,

    String vagaSetor,

    String vagaLocalizacao,

    StatusOcupacao status,

    Instant inicioEm,

    Instant fimPrevistoEm,

    Instant encerradaEm,

    String observacoes,

    UUID organizacaoId,

    Instant criadaEm,

    Instant atualizadaEm

) {

}