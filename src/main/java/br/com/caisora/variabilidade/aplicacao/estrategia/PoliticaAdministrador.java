package br.com.caisora.variabilidade.aplicacao.estrategia;

import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;

/** Administradores permitem as ações; módulos ativos são verificados pelo serviço. */
public class PoliticaAdministrador implements PoliticaPermissao {
    @Override
    public boolean permitido(ModuloSistema modulo, AcaoSistema acao) {
        // Pode retornar true direto porque o VariabilidadeService ja filtrou modulo desligado
        // e acao que o modulo nao suporta antes de chegar aqui.
        return true;
    }
}
