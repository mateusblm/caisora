package br.com.caisora.embarcacao.api;

import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmbarcacaoResponse(
        UUID id,

        UUID proprietarioId,
        String proprietarioNome,

        String nome,
        TipoEmbarcacao tipo,

        String fabricante,
        String modelo,
        Integer anoFabricacao,

        String numeroInscricao,
        String numeroCasco,
        String portoInscricao,
        String codigoPaisBandeira,

        BigDecimal comprimentoTotalMetros,
        BigDecimal bocaMetros,
        BigDecimal caladoMetros,
        BigDecimal pontalMetros,
        BigDecimal alturaTotalMetros,
        BigDecimal pesoKg,

        Integer capacidadePessoas,
        TipoPropulsao tipoPropulsao,

        String corPredominante,
        String observacoes,

        boolean ativa,

        UUID organizacaoId,
        Instant criadaEm,
        Instant atualizadaEm
) {
}