package com.app.productos.exception;

import com.app.productos.response.ApiError;
import com.app.productos.response.ApiError.ErrorCampo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Manejo centralizado de errores para todos los controllers.
 * Convierte las excepciones en el formato estándar {@link ApiError}.
 */
@Slf4j // Lombok genera el logger "log"
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Producto inexistente → 404 con el mensaje de la excepción. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ApiError.of(404, ex.getMessage()));
    }

    /** Regla de negocio violada (ej. rango de precios invertido) → 400 con el mensaje de la excepción. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleArgumentoInvalido(IllegalArgumentException ex) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.of(400, ex.getMessage()));
    }

    /**
     * Falla de {@code @Valid} → 400.
     * {@code errors} trae un elemento por campo inválido, en el orden en que se reportan.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, ApiError.of(400, "Datos inválidos", errores));
    }

    /** Falta un query param obligatorio (ej. {@code ?min=} sin {@code max}) → 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleParametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.of(400, "Falta el parámetro '" + ex.getParameterName() + "'"));
    }

    /** Parámetro con tipo incorrecto (ej. {@code ?min=abc} o {@code /api/productos/xyz}) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.of(400, "Valor inválido para el parámetro '" + ex.getName() + "'"));
    }

    /** Cuerpo JSON mal formado o ilegible → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.of(400, "El cuerpo de la petición no es un JSON válido"));
    }

    /**
     * Base de datos caída o sin responder → 503.
     * {@link CannotCreateTransactionException}: no se pudo obtener conexión al abrir la transacción
     * (vence {@code spring.datasource.hikari.connection-timeout}).
     * {@link DataAccessResourceFailureException}: se perdió la conexión durante la consulta.
     */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> handleBaseNoDisponible(Exception ex) {
        log.error("Base de datos no disponible: {}", ex.getMessage());
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                ApiError.of(503, "Base de datos no disponible, intente más tarde"));
    }

    private static ResponseEntity<ApiError> responder(HttpStatus status, ApiError error) {
        return ResponseEntity.status(status).body(error);
    }
}
