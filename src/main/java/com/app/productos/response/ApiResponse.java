package com.app.productos.response;

/**
 * Envoltorio estándar de las respuestas exitosas:
 * {@code {"status": "success", "data": ..., "message": ...}}.
 * Los errores usan {@link ApiError}.
 *
 * @param status  siempre {@code "success"}
 * @param data    el contenido de la respuesta (producto, lista, o null)
 * @param message texto opcional para el cliente; null si no aplica
 */
public record ApiResponse<T>(String status, T data, String message) {

    /** Respuesta exitosa sin mensaje. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("success", data, null);
    }

    /** Respuesta exitosa con mensaje (ej. "Producto creado"). */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>("success", data, message);
    }
}
