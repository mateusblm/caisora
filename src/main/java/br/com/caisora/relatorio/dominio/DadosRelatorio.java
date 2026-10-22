package br.com.caisora.relatorio.dominio;

import java.util.*;
import java.time.*;
import br.com.caisora.vaga.dominio.*;
import br.com.caisora.movimentacao.dominio.*;
import br.com.caisora.contrato.dominio.*;

public interface DadosRelatorio {
    record VagaOcupada(String setor, TipoVaga tipo, boolean ocupada) {
    }
    List<VagaOcupada> vagas(UUID org, String setor, TipoVaga tipo);
    List<Movimentacao> movimentacoes(UUID org, Instant inicio, Instant fimExclusivo, TipoMovimentacao tipo);
    List<Contrato> contratos(UUID org, StatusContrato status, PeriodicidadeContrato periodicidade, LocalDate inicio, LocalDate fim);
}
