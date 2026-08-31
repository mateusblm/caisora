package br.com.caisora.variabilidade.api;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.Set;

public record ModuloConfiguracaoResponse(
        ModuloSistema modulo,
        String nome,
        boolean obrigatorio,
        boolean ativo,
        Set<AcaoSistema> acoes
) {
}
