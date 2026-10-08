package br.com.caisora.variabilidade.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import br.com.caisora.variabilidade.aplicacao.estrategia.PoliticaPermissao;
import br.com.caisora.variabilidade.aplicacao.estrategia.PoliticaGerente;
import br.com.caisora.variabilidade.aplicacao.estrategia.PoliticaAtendente;
import br.com.caisora.variabilidade.aplicacao.estrategia.PoliticaFinanceiro;
import br.com.caisora.variabilidade.aplicacao.estrategia.PoliticaAdministrador;
import org.springframework.stereotype.Component;

@Component
public class PoliticaPadraoVariabilidade {
    public boolean moduloAtivo(ModuloSistema modulo) {
        return modulo != ModuloSistema.CHECKLIST_SAIDA;
    }

    public boolean permitido(PerfilUsuario perfil, ModuloSistema modulo, AcaoSistema acao) {
        return politicaPara(perfil).permitido(modulo, acao);
    }

    // Strategy: o chamador usa sempre a mesma interface.
    private PoliticaPermissao politicaPara(PerfilUsuario perfil) {
        return switch (perfil) {
            case GERENTE -> new PoliticaGerente();
            case ATENDENTE -> new PoliticaAtendente();
            case FINANCEIRO -> new PoliticaFinanceiro();
            case ADMINISTRADOR_PLATAFORMA, ADMINISTRADOR_MARINA -> new PoliticaAdministrador();
        };
    }
}
