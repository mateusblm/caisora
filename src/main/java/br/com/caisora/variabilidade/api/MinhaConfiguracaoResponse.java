package br.com.caisora.variabilidade.api;

import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.Set;

public record MinhaConfiguracaoResponse(
        Set<ModuloSistema> modulosAtivos,
        Set<String> permissoes
) {
}
