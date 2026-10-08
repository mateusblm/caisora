package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.Set;

/** Política padrão de acesso do perfil financeiro. */
public class PoliticaFinanceiro implements PoliticaPermissao {
    @Override
    public boolean permitido(ModuloSistema modulo, AcaoSistema acao) {
        return switch (modulo) {
            case DASHBOARD, CLIENTES, EMBARCACOES, OCUPACOES -> acao == AcaoSistema.VISUALIZAR;
            case CONTRATOS -> Set.of(
                    AcaoSistema.VISUALIZAR,
                    AcaoSistema.CRIAR,
                    AcaoSistema.EDITAR,
                    AcaoSistema.ALTERAR_STATUS
            ).contains(acao);
            default -> false;
        };
    }

}
