package br.com.caisora.ambiente.aplicacao;
public class ProvedorIndisponivelException extends RuntimeException {
 public ProvedorIndisponivelException() { super("Provedor ambiental indisponivel ou com resposta invalida. Tente novamente mais tarde."); }
}
