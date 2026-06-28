package br.com.caisora.autenticacao.api;

public record RespostaLogin(
        String tokenAcesso,
        String tipoToken,
        long expiraEm,
        UsuarioAutenticadoResponse usuario
) {
}
