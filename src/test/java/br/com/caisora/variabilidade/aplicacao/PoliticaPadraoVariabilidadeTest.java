package br.com.caisora.variabilidade.aplicacao;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class PoliticaPadraoVariabilidadeTest {
    // Casos de negócio que distinguem os perfis e protegem os limites de autorização.
    @ParameterizedTest
    @CsvSource({
        "ADMINISTRADOR_MARINA, USUARIOS, CRIAR, true",
        "ADMINISTRADOR_PLATAFORMA, CONFIGURACOES, CONFIGURAR, true",
        "GERENTE, MOVIMENTACOES, CONCLUIR, true",
        "GERENTE, USUARIOS, VISUALIZAR, false",
        "GERENTE, CONFIGURACOES, CONFIGURAR, false",
        "GERENTE, CHECKLIST_SAIDA, CONCLUIR, true",
        "GERENTE, CHECKLIST_SAIDA, CRIAR, false",
        "ATENDENTE, CLIENTES, CRIAR, true",
        "ATENDENTE, EMBARCACOES, EDITAR, true",
        "ATENDENTE, MOVIMENTACOES, CRIAR, true",
        "ATENDENTE, MOVIMENTACOES, INICIAR, false",
        "ATENDENTE, MOVIMENTACOES, CONCLUIR, false",
        "ATENDENTE, VAGAS, CRIAR, false",
        "ATENDENTE, CONTRATOS, EDITAR, false",
        "FINANCEIRO, CONTRATOS, ALTERAR_STATUS, true",
        "FINANCEIRO, OCUPACOES, VISUALIZAR, true",
        "FINANCEIRO, CLIENTES, EDITAR, false",
        "FINANCEIRO, MOVIMENTACOES, VISUALIZAR, false"
    })
    void deveAplicarPoliticaDoPerfil(PerfilUsuario perfil, ModuloSistema modulo,
            AcaoSistema acao, boolean permitido) {
        assertThat(new PoliticaPadraoVariabilidade().permitido(perfil, modulo, acao))
            .isEqualTo(permitido);
    }
}
