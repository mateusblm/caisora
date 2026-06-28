package br.com.caisora.compartilhado.excecao;

import java.time.Instant;
import java.util.List;

public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String codigo,
        String mensagem,
        String caminho,
        List<ErroCampoResponse> errosCampos
) {
}
