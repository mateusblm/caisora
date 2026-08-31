package br.com.caisora.variabilidade.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PoliticaPadraoVariabilidade {

    public boolean moduloAtivo(ModuloSistema modulo) {
        return modulo != ModuloSistema.CHECKLIST_SAIDA;
    }

    public boolean permitido(PerfilUsuario perfil, ModuloSistema modulo, AcaoSistema acao) {
        if (perfil == PerfilUsuario.ADMINISTRADOR_PLATAFORMA
                || perfil == PerfilUsuario.ADMINISTRADOR_MARINA) {
            return true;
        }

        return switch (perfil) {
            case GERENTE -> permitidoGerente(modulo, acao);
            case ATENDENTE -> permitidoAtendente(modulo, acao);
            case FINANCEIRO -> permitidoFinanceiro(modulo, acao);
            case ADMINISTRADOR_PLATAFORMA, ADMINISTRADOR_MARINA -> true;
        };
    }

    private boolean permitidoGerente(ModuloSistema modulo, AcaoSistema acao) {
        if (modulo == ModuloSistema.USUARIOS || modulo == ModuloSistema.CONFIGURACOES) {
            return false;
        }
        if (modulo == ModuloSistema.CHECKLIST_SAIDA) {
            return Set.of(AcaoSistema.VISUALIZAR, AcaoSistema.EDITAR, AcaoSistema.CONCLUIR).contains(acao);
        }
        return acoesOperacionais().contains(acao) || acao == AcaoSistema.ALTERAR_STATUS;
    }

    private boolean permitidoAtendente(ModuloSistema modulo, AcaoSistema acao) {
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

    private boolean permitidoFinanceiro(ModuloSistema modulo, AcaoSistema acao) {
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
