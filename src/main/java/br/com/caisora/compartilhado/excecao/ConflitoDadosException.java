package br.com.caisora.compartilhado.excecao;

public class ConflitoDadosException extends RuntimeException {

    public ConflitoDadosException(String mensagem) {
        super(mensagem);
    }
}
