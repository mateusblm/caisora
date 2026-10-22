package br.com.caisora.referencia.aplicacao;

public class ReferenciaIndisponivelException extends RuntimeException {
    public ReferenciaIndisponivelException() {
        super("Serviço de referência externo indisponível ou com resposta inválida. Tente novamente mais tarde.");
    }
}
