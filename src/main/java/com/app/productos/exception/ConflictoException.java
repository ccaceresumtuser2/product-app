package com.app.productos.exception;

/**
 * Se lanza cuando la operación es ambigua o choca con el estado actual de los datos
 * (por ejemplo, varios productos con el mismo nombre).
 * {@link GlobalExceptionHandler} la traduce a una respuesta HTTP 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
