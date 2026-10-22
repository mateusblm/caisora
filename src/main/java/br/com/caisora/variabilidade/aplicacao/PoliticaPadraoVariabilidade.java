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

// Comportamento "de fabrica" de toda marina. So e consultado quando a marina
// nao tem configuracao propria salva para aquele modulo ou permissao.
@Component
public class PoliticaPadraoVariabilidade {
    // Novos módulos e checklist exigem liberação explícita pela plataforma.
    public boolean moduloAtivo(ModuloSistema modulo) {
        return modulo != ModuloSistema.CHECKLIST_SAIDA
                && modulo != ModuloSistema.AMBIENTE && modulo != ModuloSistema.RELATORIOS && modulo != ModuloSistema.REFERENCIAS;
    }

    public boolean permitido(PerfilUsuario perfil, ModuloSistema modulo, AcaoSistema acao) {
        return politicaPara(perfil).permitido(modulo, acao);
    }

    // O chamador usa sempre a mesma interface.
    // Os dois administradores caem na mesma politica; o que diferencia o da plataforma
    // e um atalho no VariabilidadeService, antes de chegar aqui.
    private PoliticaPermissao politicaPara(PerfilUsuario perfil) {
        return switch (perfil) {
            case GERENTE -> new PoliticaGerente();
            case ATENDENTE -> new PoliticaAtendente();
            case FINANCEIRO -> new PoliticaFinanceiro();
            case ADMINISTRADOR_PLATAFORMA, ADMINISTRADOR_MARINA -> new PoliticaAdministrador();
        };
    }
}
