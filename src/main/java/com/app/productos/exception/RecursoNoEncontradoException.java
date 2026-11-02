package com.app.productos.exception;

/**
 * Se lanza cuando se busca un recurso por id y no existe.
 * {@link GlobalExceptionHandler} la traduce a una respuesta HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
