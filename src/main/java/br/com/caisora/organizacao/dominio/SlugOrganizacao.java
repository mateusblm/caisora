package br.com.caisora.organizacao.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugOrganizacao {

    public static final int TAMANHO_MAXIMO = 80;

    private static final Pattern FORMATO_VALIDO =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private SlugOrganizacao() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Codigo da marina e obrigatorio");
        }

        String normalizado = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        validar(normalizado);

        return normalizado;
    }

    public static void validar(String valorNormalizado) {
        if (valorNormalizado == null || valorNormalizado.isBlank()) {
            throw new IllegalArgumentException("Codigo da marina e obrigatorio");
        }

        if (valorNormalizado.length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Codigo da marina deve ter no maximo 80 caracteres");
        }

        if (!FORMATO_VALIDO.matcher(valorNormalizado).matches()) {
            throw new IllegalArgumentException("Codigo da marina deve conter letras, numeros e hifens simples");
        }
    }
}
