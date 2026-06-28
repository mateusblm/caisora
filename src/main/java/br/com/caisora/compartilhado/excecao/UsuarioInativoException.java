package br.com.caisora.compartilhado.excecao;

public class UsuarioInativoException extends RuntimeException {

    public UsuarioInativoException() {
        super("Usuario inativo");
    }
}
