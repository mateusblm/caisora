package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;

/** Strategy: cada perfil responde à mesma pergunta com sua política de acesso. */
public interface PoliticaPermissao {
    boolean permitido(ModuloSistema modulo, AcaoSistema acao);
}
