package br.com.caisora.variabilidade.api;

import java.util.List;

public record ConfiguracaoVariabilidadeResponse(
        List<ModuloConfiguracaoResponse> modulos,
        List<PerfilConfiguracaoResponse> perfis
) {
}
