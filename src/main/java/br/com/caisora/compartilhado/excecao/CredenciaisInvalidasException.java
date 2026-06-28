package br.com.caisora.compartilhado.excecao;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha invalidos");
    }
}
