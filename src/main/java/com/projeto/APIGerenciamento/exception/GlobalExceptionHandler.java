package com.projeto.APIGerenciamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404: recurso não encontrado ────────────────────────────────────
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, ex.getMessage()));
    }

    // ── 409: conflito de reserva ───────────────────────────────────────
    @ExceptionHandler(ConflitoDeReservaException.class)
    public ResponseEntity<ErroResponse> handleConflito(
            ConflitoDeReservaException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(409, ex.getMessage()));
    }

    // ── 422: regra de negócio violada ─────────────────────────────────
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.de(422, ex.getMessage()));
    }

    // ── 400: falha de validação do Bean Validation (@Valid) ───────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacaoResponse> handleValidacao(
            MethodArgumentNotValidException ex) {

        Map<String, String> campos = new HashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroValidacaoResponse(400, "Erro de validação", campos,
                        LocalDateTime.now()));
    }

    // ── registros de resposta de erro ─────────────────────────────────

    public record ErroResponse(int status, String mensagem, LocalDateTime timestamp) {
        public static ErroResponse de(int status, String mensagem) {
            return new ErroResponse(status, mensagem, LocalDateTime.now());
        }
    }

    public record ErroValidacaoResponse(
            int status,
            String mensagem,
            Map<String, String> campos,
            LocalDateTime timestamp
    ) {}
}