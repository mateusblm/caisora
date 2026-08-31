package br.com.caisora.variabilidade.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloOrganizacao;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import br.com.caisora.variabilidade.dominio.PermissaoPerfilOrganizacao;
import br.com.caisora.variabilidade.infraestrutura.ModuloOrganizacaoRepositoryJpa;
import br.com.caisora.variabilidade.infraestrutura.PermissaoPerfilOrganizacaoRepositoryJpa;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VariabilidadeServiceTest {

    private ModuloOrganizacaoRepositoryJpa moduloRepository;
    private PermissaoPerfilOrganizacaoRepositoryJpa permissaoRepository;
    private VariabilidadeService service;

    @BeforeEach
    void configurar() {
        moduloRepository = mock(ModuloOrganizacaoRepositoryJpa.class);
        permissaoRepository = mock(PermissaoPerfilOrganizacaoRepositoryJpa.class);
        service = new VariabilidadeService(
                moduloRepository,
                permissaoRepository,
                new PoliticaPadraoVariabilidade()
        );
    }

    @Test
    void deveUsarPoliticaPadraoQuandoNaoExistirCustomizacao() {
        UUID organizacaoId = UUID.randomUUID();
        when(moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, ModuloSistema.CLIENTES))
                .thenReturn(Optional.empty());
        when(permissaoRepository.findByOrganizacaoIdAndPerfilAndModuloAndAcao(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                ModuloSistema.CLIENTES,
                AcaoSistema.CRIAR
        )).thenReturn(Optional.empty());

        assertThat(service.permitido(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                ModuloSistema.CLIENTES,
                AcaoSistema.CRIAR
        )).isTrue();
    }

    @Test
    void deveBloquearAcaoQuandoModuloEstiverDesativadoParaOrganizacao() {
        UUID organizacaoId = UUID.randomUUID();
        ModuloOrganizacao configuracao = ModuloOrganizacao.criar(
                organizacaoId,
                ModuloSistema.CONTRATOS,
                false
        );
        when(moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, ModuloSistema.CONTRATOS))
                .thenReturn(Optional.of(configuracao));

        assertThat(service.permitido(
                organizacaoId,
                PerfilUsuario.ADMINISTRADOR_MARINA,
                ModuloSistema.CONTRATOS,
                AcaoSistema.VISUALIZAR
        )).isFalse();
    }

    @Test
    void deveAplicarOverrideDePermissaoPorPerfil() {
        UUID organizacaoId = UUID.randomUUID();
        when(moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, ModuloSistema.CLIENTES))
                .thenReturn(Optional.empty());
        PermissaoPerfilOrganizacao override = PermissaoPerfilOrganizacao.criar(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                ModuloSistema.CLIENTES,
                AcaoSistema.CRIAR,
                false
        );
        when(permissaoRepository.findByOrganizacaoIdAndPerfilAndModuloAndAcao(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                ModuloSistema.CLIENTES,
                AcaoSistema.CRIAR
        )).thenReturn(Optional.of(override));

        assertThat(service.permitido(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                ModuloSistema.CLIENTES,
                AcaoSistema.CRIAR
        )).isFalse();
    }

    @Test
    void deveManterConfiguracoesComoModuloObrigatorio() {
        UUID organizacaoId = UUID.randomUUID();

        assertThatThrownBy(() -> service.atualizarModulo(
                organizacaoId,
                ModuloSistema.CONFIGURACOES,
                false
        ))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    void devePermitirAtivarChecklistSomenteParaOrganizacaoConfigurada() {
        UUID organizacaoId = UUID.randomUUID();
        when(moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, ModuloSistema.CHECKLIST_SAIDA))
                .thenReturn(Optional.empty());

        assertThat(service.moduloAtivo(organizacaoId, ModuloSistema.CHECKLIST_SAIDA)).isFalse();

        service.atualizarModulo(organizacaoId, ModuloSistema.CHECKLIST_SAIDA, true);

        verify(moduloRepository).save(org.mockito.ArgumentMatchers.any(ModuloOrganizacao.class));
    }

    @Test
    void deveRejeitarPermissaoQueNaoExisteNoModulo() {
        UUID organizacaoId = UUID.randomUUID();
        when(permissaoRepository.findAllByOrganizacaoIdAndPerfil(
                organizacaoId,
                PerfilUsuario.ATENDENTE
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.atualizarPermissoes(
                organizacaoId,
                PerfilUsuario.ATENDENTE,
                Set.of("CLIENTES:INICIAR")
        ))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Permissao invalida");
    }
}
