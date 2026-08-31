package br.com.caisora.variabilidade.dominio;

import java.util.EnumSet;
import java.util.Set;

public enum ModuloSistema {
    DASHBOARD("Dashboard", true, AcaoSistema.VISUALIZAR),
    CLIENTES("Clientes", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.ALTERAR_STATUS),
    EMBARCACOES("Embarcacoes", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.ALTERAR_STATUS),
    VAGAS("Vagas", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.ALTERAR_STATUS),
    OCUPACOES("Ocupacoes", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.CONCLUIR),
    MOVIMENTACOES("Movimentacoes", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR,
            AcaoSistema.INICIAR, AcaoSistema.CONCLUIR, AcaoSistema.CANCELAR),
    CONTRATOS("Contratos", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.ALTERAR_STATUS),
    PAINEL_TV("Painel TV", false, AcaoSistema.VISUALIZAR),
    USUARIOS("Usuarios", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.CRIAR, AcaoSistema.EDITAR, AcaoSistema.ALTERAR_STATUS),
    CHECKLIST_SAIDA("Checklist de saida", false,
            AcaoSistema.VISUALIZAR, AcaoSistema.EDITAR, AcaoSistema.CONCLUIR),
    CONFIGURACOES("Configuracoes", true, AcaoSistema.CONFIGURAR);

    private final String nomeExibicao;
    private final boolean obrigatorio;
    private final Set<AcaoSistema> acoesSuportadas;

    ModuloSistema(String nomeExibicao, boolean obrigatorio, AcaoSistema... acoesSuportadas) {
        this.nomeExibicao = nomeExibicao;
        this.obrigatorio = obrigatorio;
        this.acoesSuportadas = acoesSuportadas.length == 0
                ? EnumSet.noneOf(AcaoSistema.class)
                : EnumSet.copyOf(Set.of(acoesSuportadas));
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public boolean isObrigatorio() {
        return obrigatorio;
    }

    public Set<AcaoSistema> getAcoesSuportadas() {
        return Set.copyOf(acoesSuportadas);
    }

    public boolean suporta(AcaoSistema acao) {
        return acoesSuportadas.contains(acao);
    }
}
