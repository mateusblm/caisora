package br.com.caisora.relatorio.api;

import java.math.BigDecimal;
import java.util.*;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.contrato.dominio.*;

public final class RelatoriosResponse {
    private RelatoriosResponse() {
    }
    public record Ocupacao(long totalVagasAtivas,long ocupadas,long disponiveis,BigDecimal taxaOcupacao,List<GrupoOcupacao> grupos) {
    }
    public record GrupoOcupacao(String setor,TipoVaga tipo,long totalVagasAtivas,long ocupadas,long disponiveis,BigDecimal taxaOcupacao) {
    }
    public record Movimentacoes(long total,Map<StatusMovimentacao,Long> porSituacao,long concluidas,long canceladas,
    BigDecimal tempoMedioExecucaoSegundos,long amostrasDuracao,long concluidasNoPrazo,BigDecimal percentualNoPrazo,String regraPontualidade) {
    }
    public record Contratos(long total,Map<StatusContrato,Long> porSituacao,Map<StatusContrato,BigDecimal> percentualPorSituacao,
    Map<PeriodicidadeContrato,Long> porPeriodicidade,Map<PeriodicidadeContrato,BigDecimal> valoresBasePorPeriodicidade) {
    }
}
