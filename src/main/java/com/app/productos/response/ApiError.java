package com.app.productos.response;

import java.util.List;

/**
 * Envoltorio estándar de las respuestas de error:
 * {@code {"status": "error", "code": 404, "message": ..., "errors": [...]}}.
 * Lo construye {@code GlobalExceptionHandler}.
 *
 * @param status  siempre {@code "error"}
 * @param code    código HTTP (se repite en el cuerpo para comodidad del cliente)
 * @param message descripción general del error
 * @param errors  detalle por campo (validaciones); lista vacía si no aplica
 */
public record ApiError(String status, int code, String message, List<ErrorCampo> errors) {

    /** Error de un campo puntual, ej. {@code {"campo": "nombre", "mensaje": "El nombre es obligatorio"}}. */
    public record ErrorCampo(String campo, String mensaje) {
    }

    public static ApiError of(int code, String message) {
        return new ApiError("error", code, message, List.of());
    }

    public static ApiError of(int code, String message, List<ErrorCampo> errors) {
        return new ApiError("error", code, message, errors);
    }
}
