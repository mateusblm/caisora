package br.com.caisora.compartilhado.excecao;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Codigo da marina, e-mail ou senha invalidos");
    }
}
