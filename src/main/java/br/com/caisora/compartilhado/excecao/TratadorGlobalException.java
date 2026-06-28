package br.com.caisora.compartilhado.excecao;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorGlobalException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErroCampoResponse> errosCampos = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(criarErro(
                HttpStatus.BAD_REQUEST,
                "ERRO_VALIDACAO",
                "Dados invalidos",
                request.getRequestURI(),
                errosCampos));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroResponse> tratarHeaderObrigatorioAusente(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(criarErro(
                HttpStatus.BAD_REQUEST,
                "HEADER_OBRIGATORIO_AUSENTE",
                "Header obrigatorio ausente: " + exception.getHeaderName(),
                request.getRequestURI(),
                List.of(new ErroCampoResponse(exception.getHeaderName(), "Header obrigatorio"))));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(
            TypeMismatchException exception,
            HttpServletRequest request
    ) {
        String campo = exception.getPropertyName() == null ? "parametro" : exception.getPropertyName();
        return ResponseEntity.badRequest().body(criarErro(
                HttpStatus.BAD_REQUEST,
                "TIPO_INVALIDO",
                "Parametro com formato invalido",
                request.getRequestURI(),
                List.of(new ErroCampoResponse(campo, "Formato invalido"))));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(
            CredenciaisInvalidasException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "CREDENCIAIS_INVALIDAS",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(UsuarioInativoException.class)
    public ResponseEntity<ErroResponse> tratarUsuarioInativo(
            UsuarioInativoException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "USUARIO_INATIVO",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(OrganizacaoInativaException.class)
    public ResponseEntity<ErroResponse> tratarOrganizacaoInativa(
            OrganizacaoInativaException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "ORGANIZACAO_INATIVA",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(ConflitoDadosException.class)
    public ResponseEntity<ErroResponse> tratarConflitoDados(
            ConflitoDadosException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "CONFLITO_DADOS",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "RECURSO_NAO_ENCONTRADO",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(criarErro(
                status,
                "ERRO_INTERNO",
                "Erro inesperado",
                request.getRequestURI(),
                List.of()));
    }

    private ErroResponse criarErro(
            HttpStatus status,
            String codigo,
            String mensagem,
            String caminho,
            List<ErroCampoResponse> errosCampos
    ) {
        return new ErroResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                codigo,
                mensagem,
                caminho,
                errosCampos);
    }
}
