package br.com.caisora.compartilhado.excecao;

public class OrganizacaoInativaException extends RuntimeException {

    public OrganizacaoInativaException() {
        super("Organizacao inativa");
    }
}
