package br.com.caisora.relatorio.api;

import br.com.caisora.vaga.dominio.TipoVaga;
import java.math.BigDecimal;
import java.util.List;

public record PermanenciaVagasResponse(
        Indicadores resumo,
        List<Grupo> grupos
) {
    public record Indicadores(
            long quantidadeOcupacoes,
            BigDecimal permanenciaMediaSegundos,
            BigDecimal menorPermanenciaSegundos,
            BigDecimal maiorPermanenciaSegundos,
            BigDecimal totalHorasOcupacao
    ) {
    }

    public record Grupo(String setor, TipoVaga tipo, Indicadores indicadores) {
    }
}
