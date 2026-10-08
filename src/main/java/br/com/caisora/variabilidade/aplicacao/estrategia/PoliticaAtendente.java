package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.Set;

/** Política padrão de acesso do perfil atendente. */
public class PoliticaAtendente implements PoliticaPermissao {
    @Override
    public boolean permitido(ModuloSistema modulo, AcaoSistema acao) {
        return switch (modulo) {
            case DASHBOARD, PAINEL_TV -> acao == AcaoSistema.VISUALIZAR;
            case CLIENTES, EMBARCACOES -> Set.of(
                    AcaoSistema.VISUALIZAR,
                    AcaoSistema.CRIAR,
                    AcaoSistema.EDITAR
            ).contains(acao);
            case VAGAS -> acao == AcaoSistema.VISUALIZAR;
            case OCUPACOES, MOVIMENTACOES -> Set.of(
                    AcaoSistema.VISUALIZAR,
                    AcaoSistema.CRIAR,
                    AcaoSistema.EDITAR
            ).contains(acao);
            case CONTRATOS -> acao == AcaoSistema.VISUALIZAR;
            case CHECKLIST_SAIDA -> Set.of(AcaoSistema.VISUALIZAR, AcaoSistema.EDITAR).contains(acao);
            case USUARIOS, CONFIGURACOES -> false;
        };
    }

}
