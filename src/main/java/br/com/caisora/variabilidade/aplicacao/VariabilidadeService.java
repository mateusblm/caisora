package br.com.caisora.variabilidade.aplicacao;

import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.api.ConfiguracaoVariabilidadeResponse;
import br.com.caisora.variabilidade.api.MinhaConfiguracaoResponse;
import br.com.caisora.variabilidade.api.ModuloConfiguracaoResponse;
import br.com.caisora.variabilidade.api.PerfilConfiguracaoResponse;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloOrganizacao;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import br.com.caisora.variabilidade.dominio.PermissaoPerfilOrganizacao;
import br.com.caisora.variabilidade.infraestrutura.ModuloOrganizacaoRepositoryJpa;
import br.com.caisora.variabilidade.infraestrutura.PermissaoPerfilOrganizacaoRepositoryJpa;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// As tabelas organizacao_modulos e perfil_permissoes guardam so o que a marina
// mudou em relacao ao padrao. Sem linha no banco, vale a PoliticaPadraoVariabilidade.
// Assim uma marina nova funciona sem precisar de nenhum cadastro inicial.
@Service
public class VariabilidadeService {

    private final ModuloOrganizacaoRepositoryJpa moduloRepository;
    private final PermissaoPerfilOrganizacaoRepositoryJpa permissaoRepository;
    private final PoliticaPadraoVariabilidade politicaPadrao;

    public VariabilidadeService(
            ModuloOrganizacaoRepositoryJpa moduloRepository,
            PermissaoPerfilOrganizacaoRepositoryJpa permissaoRepository,
            PoliticaPadraoVariabilidade politicaPadrao
    ) {
        this.moduloRepository = moduloRepository;
        this.permissaoRepository = permissaoRepository;
        this.politicaPadrao = politicaPadrao;
    }

    @Transactional(readOnly = true)
    public boolean moduloAtivo(UUID organizacaoId, ModuloSistema modulo) {
        // Dashboard e Configuracoes nao podem sumir, senao o admin perde o acesso a tela que reativa os modulos.
        if (modulo.isObrigatorio()) {
            return true;
        }
        return moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, modulo)
                .map(ModuloOrganizacao::isAtivo)
                .orElseGet(() -> politicaPadrao.moduloAtivo(modulo));
    }

    @Transactional(readOnly = true)
    public boolean permitido(
            UUID organizacaoId,
            PerfilUsuario perfil,
            ModuloSistema modulo,
            AcaoSistema acao
    ) {
        // A ordem dos ifs importa:
        // 1. acao que o modulo nem tem (ex.: PAINEL_TV:CRIAR) e sempre negada
        // 2. admin da plataforma passa direto, ele nao pertence a uma marina
        // 3. modulo desligado bloqueia tudo dele, mesmo com permissao salva
        // 4. permissao customizada da marina; se nao tiver, cai na politica do perfil
        if (!modulo.suporta(acao)) {
            return false;
        }
        if (perfil == PerfilUsuario.ADMINISTRADOR_PLATAFORMA) {
            return true;
        }
        if (!moduloAtivo(organizacaoId, modulo)) {
            return false;
        }
        return permissaoRepository
                .findByOrganizacaoIdAndPerfilAndModuloAndAcao(organizacaoId, perfil, modulo, acao)
                .map(PermissaoPerfilOrganizacao::isPermitido)
                .orElseGet(() -> politicaPadrao.permitido(perfil, modulo, acao));
    }

    @Transactional(readOnly = true)
    public MinhaConfiguracaoResponse obterMinhaConfiguracao(UsuarioAutenticado usuario) {
        return obterMinhaConfiguracao(usuario.organizacaoId(), usuario.perfil());
    }

    @Transactional(readOnly = true)
    public MinhaConfiguracaoResponse obterMinhaConfiguracao(UUID organizacaoId, PerfilUsuario perfil) {
        // Vai no login e no /me. O frontend usa essas listas so para esconder menu e botao;
        // quem bloqueia de verdade e o @PreAuthorize nos controllers.
        Set<ModuloSistema> modulosAtivos = Arrays.stream(ModuloSistema.values())
                .filter(modulo -> moduloAtivo(organizacaoId, modulo))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ModuloSistema.class)));

        Set<String> permissoes = new LinkedHashSet<>();
        for (ModuloSistema modulo : ModuloSistema.values()) {
            for (AcaoSistema acao : modulo.getAcoesSuportadas()) {
                if (permitido(organizacaoId, perfil, modulo, acao)) {
                    permissoes.add(chavePermissao(modulo, acao));
                }
            }
        }

        return new MinhaConfiguracaoResponse(modulosAtivos, permissoes);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoVariabilidadeResponse obterConfiguracao(UUID organizacaoId) {
        List<ModuloConfiguracaoResponse> modulos = Arrays.stream(ModuloSistema.values())
                .map(modulo -> new ModuloConfiguracaoResponse(
                        modulo,
                        modulo.getNomeExibicao(),
                        modulo.isObrigatorio(),
                        moduloAtivo(organizacaoId, modulo),
                        modulo.getAcoesSuportadas()
                ))
                .toList();

        List<PerfilConfiguracaoResponse> perfis = Arrays.stream(PerfilUsuario.values())
                .filter(perfil -> perfil != PerfilUsuario.ADMINISTRADOR_PLATAFORMA)
                .map(perfil -> new PerfilConfiguracaoResponse(
                        perfil,
                        obterMinhaConfiguracao(organizacaoId, perfil).permissoes()
                ))
                .toList();

        return new ConfiguracaoVariabilidadeResponse(modulos, perfis);
    }

    @Transactional
    public ConfiguracaoVariabilidadeResponse atualizarModulo(
            UUID organizacaoId,
            ModuloSistema modulo,
            boolean ativo
    ) {
        if (modulo.isObrigatorio() && !ativo) {
            throw new DadosInvalidosException(
                    "MODULO_OBRIGATORIO",
                    "O modulo " + modulo.getNomeExibicao() + " e obrigatorio e nao pode ser desativado"
            );
        }

        boolean padrao = politicaPadrao.moduloAtivo(modulo);
        var existente = moduloRepository.findByOrganizacaoIdAndModulo(organizacaoId, modulo);

        // Voltou pro valor padrao: apaga a linha em vez de salvar um registro que nao muda nada.
        if (ativo == padrao) {
            existente.ifPresent(moduloRepository::delete);
        } else if (existente.isPresent()) {
            existente.get().alterar(ativo);
        } else {
            moduloRepository.save(ModuloOrganizacao.criar(organizacaoId, modulo, ativo));
        }

        return obterConfiguracao(organizacaoId);
    }

    @Transactional
    public ConfiguracaoVariabilidadeResponse atualizarPermissoes(
            UUID organizacaoId,
            PerfilUsuario perfil,
            Set<String> chavesPermitidas
    ) {
        if (perfil == PerfilUsuario.ADMINISTRADOR_PLATAFORMA) {
            throw new DadosInvalidosException(
                    "PERFIL_NAO_CONFIGURAVEL",
                    "O administrador da plataforma nao pode ter suas permissoes alteradas por uma marina"
            );
        }

        Set<String> normalizadas = chavesPermitidas.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String chave : normalizadas) {
            validarChavePermissao(chave);
        }

        // A tela manda a lista completa do que o perfil pode fazer, nao so o que mudou.
        // Por isso apaga tudo do perfil e grava de novo so as diferencas em relacao ao padrao.
        List<PermissaoPerfilOrganizacao> existentes =
                permissaoRepository.findAllByOrganizacaoIdAndPerfil(organizacaoId, perfil);
        permissaoRepository.deleteAll(existentes);
        // O Hibernate roda os inserts antes dos deletes no flush. Sem forcar o delete aqui,
        // regravar a mesma chave estoura a unique uk_perfil_permissao.
        permissaoRepository.flush();

        for (ModuloSistema modulo : ModuloSistema.values()) {
            for (AcaoSistema acao : modulo.getAcoesSuportadas()) {
                boolean desejado = normalizadas.contains(chavePermissao(modulo, acao));
                // Trava: o admin da marina nunca perde CONFIGURACOES:CONFIGURAR,
                // senao ele mesmo se tranca fora da tela de permissoes.
                if (perfil == PerfilUsuario.ADMINISTRADOR_MARINA
                        && modulo == ModuloSistema.CONFIGURACOES
                        && acao == AcaoSistema.CONFIGURAR) {
                    desejado = true;
                }
                boolean padrao = politicaPadrao.permitido(perfil, modulo, acao);
                if (desejado != padrao) {
                    permissaoRepository.save(PermissaoPerfilOrganizacao.criar(
                            organizacaoId,
                            perfil,
                            modulo,
                            acao,
                            desejado
                    ));
                }
            }
        }

        return obterConfiguracao(organizacaoId);
    }

    // Formato "MODULO:ACAO" (ex.: CLIENTES:CRIAR). O frontend monta a mesma string em temPermissao().
    public static String chavePermissao(ModuloSistema modulo, AcaoSistema acao) {
        return modulo.name() + ":" + acao.name();
    }

    private void validarChavePermissao(String chave) {
        // Com limit -1 o split mantem partes vazias no fim: "CLIENTES:CRIAR:" vira 3 partes e e rejeitada.
        // Sem o -1 ela viraria 2 partes e passaria como valida.
        String[] partes = chave.split(":", -1);
        if (partes.length != 2) {
            throw chaveInvalida(chave);
        }
        try {
            ModuloSistema modulo = ModuloSistema.valueOf(partes[0]);
            AcaoSistema acao = AcaoSistema.valueOf(partes[1]);
            if (!modulo.suporta(acao)) {
                throw chaveInvalida(chave);
            }
        } catch (IllegalArgumentException exception) {
            throw chaveInvalida(chave);
        }
    }

    private DadosInvalidosException chaveInvalida(String chave) {
        return new DadosInvalidosException(
                "PERMISSAO_INVALIDA",
                "Permissao invalida: " + chave
        );
    }
}
