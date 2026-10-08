package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.EnumSet;
import java.util.Set;

/** Política padrão de acesso do perfil gerente. */
public class PoliticaGerente implements PoliticaPermissao {
    @Override
    public boolean permitido(ModuloSistema modulo, AcaoSistema acao) {
        if (modulo == ModuloSistema.USUARIOS || modulo == ModuloSistema.CONFIGURACOES) {
            return false;
        }
        if (modulo == ModuloSistema.CHECKLIST_SAIDA) {
            return Set.of(AcaoSistema.VISUALIZAR, AcaoSistema.EDITAR, AcaoSistema.CONCLUIR).contains(acao);
        }
        return acoesOperacionais().contains(acao) || acao == AcaoSistema.ALTERAR_STATUS;
    }

    private Set<AcaoSistema> acoesOperacionais() {
        return EnumSet.of(
                AcaoSistema.VISUALIZAR,
                AcaoSistema.CRIAR,
                AcaoSistema.EDITAR,
                AcaoSistema.INICIAR,
                AcaoSistema.CONCLUIR,
                AcaoSistema.CANCELAR
        );
    }
}
