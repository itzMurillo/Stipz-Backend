package br.com.stipz.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(
            RegraNegocioException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                new ErroResponse(
                        LocalDateTime.now(),
                        400,
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(404).body(
                new ErroResponse(
                        LocalDateTime.now(),
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
                new ErroResponse(
                        LocalDateTime.now(),
                        400,
                        mensagem,
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> validacaoParametro(
            jakarta.validation.ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String mensagem = ex.getConstraintViolations().stream()
                .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                .collect(Collectors.joining("; "));

        return erro(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return erro(HttpStatus.BAD_REQUEST, mensagemJsonInvalido(ex), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                "Valor inválido para o campo '" + ex.getName() + "'",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroObrigatorioAusente(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: " + ex.getParameterName(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return erro(HttpStatus.FORBIDDEN, "Você não tem permissão para executar esta operação", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        String mensagem = ex instanceof BadCredentialsException
                ? "Email ou senha inválidos"
                : "Autenticação obrigatória";

        return erro(HttpStatus.UNAUTHORIZED, mensagem, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> midiaNaoSuportada(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaNaoEncontrada(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return erro(HttpStatus.NOT_FOUND, "Rota não encontrada", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        ex.printStackTrace();

        return ResponseEntity.status(500).body(
                new ErroResponse(
                        LocalDateTime.now(),
                        500,
                        "Erro interno do servidor",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String mensagem = "Erro de integridade dos dados";

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraint) {
                mensagem = "Violação de regra do banco: " + constraint.getConstraintName();
                break;
            }
            cause = cause.getCause();
        }

        return ResponseEntity.badRequest().body(
                new ErroResponse(
                        LocalDateTime.now(),
                        400,
                        mensagem,
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> conflitoConcorrente(
            PessimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        return erro(
                HttpStatus.CONFLICT,
                "Outro usuário alterou estes dados ao mesmo tempo. Atualize e tente novamente",
                request
        );
    }

    private String mensagemJsonInvalido(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();

        if (causa instanceof InvalidFormatException invalidFormatException
                && invalidFormatException.getPath() != null
                && !invalidFormatException.getPath().isEmpty()) {
            String campo = invalidFormatException.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));

            return "Valor inválido para o campo '" + campo + "'";
        }

        String mensagem = causa.getMessage();
        if (mensagem != null && mensagem.contains("Required request body is missing")) {
            return "Corpo da requisição é obrigatório";
        }

        return "JSON inválido ou campo com tipo incorreto";
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ErroResponse(
                        LocalDateTime.now(),
                        status.value(),
                        mensagem,
                        request.getRequestURI()
                )
        );
    }
}
