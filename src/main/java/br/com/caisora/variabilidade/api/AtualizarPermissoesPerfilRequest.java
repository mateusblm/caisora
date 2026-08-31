package br.com.caisora.variabilidade.api;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AtualizarPermissoesPerfilRequest(
        @NotNull Set<String> permissoes
) {
}
