package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;

/** Administradores permitem as ações; módulos ativos são verificados pelo serviço. */
public class PoliticaAdministrador implements PoliticaPermissao {
    @Override
    public boolean permitido(ModuloSistema modulo, AcaoSistema acao) {
        return true;
    }
}
