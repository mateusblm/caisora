package br.com.caisora.compartilhado.excecao;

public record ErroCampoResponse(
        String campo,
        String mensagem
) {
}
