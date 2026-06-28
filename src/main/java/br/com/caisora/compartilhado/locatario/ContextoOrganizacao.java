package br.com.caisora.compartilhado.locatario;

import java.util.Optional;
import java.util.UUID;

public final class ContextoOrganizacao {

    private static final ThreadLocal<UUID> ORGANIZACAO_ATUAL = new ThreadLocal<>();

    private ContextoOrganizacao() {
    }

    public static void definir(UUID organizacaoId) {
        ORGANIZACAO_ATUAL.set(organizacaoId);
    }

    public static Optional<UUID> obter() {
        return Optional.ofNullable(ORGANIZACAO_ATUAL.get());
    }

    public static UUID obterObrigatorio() {
        return obter().orElseThrow(() -> new IllegalStateException("Organizacao nao definida no contexto atual"));
    }

    public static void limpar() {
        ORGANIZACAO_ATUAL.remove();
    }
}
