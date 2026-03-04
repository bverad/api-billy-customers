package com.billy.customers.common;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapeo centralizado de excepciones a código HTTP y mensaje seguro para el cliente.
 * Ver regla api-error-handling-resilience.mdc.
 */
public final class ErrorMapping {

    private static final Logger LOGGER = Logger.getLogger(ErrorMapping.class.getName());

    private ErrorMapping() {
    }

    /**
     * Resultado del mapeo: código HTTP y mensaje para el cuerpo de respuesta.
     */
    public record Result(int status, String clientMessage) {
    }

    /**
     * Mapea la excepción a status y mensaje; registra en log con nivel adecuado.
     */
    public static Result from(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            LOGGER.warning(() -> "Validación: " + t.getMessage());
            return new Result(400, t.getMessage() != null ? t.getMessage() : "Bad request");
        }
        if (t instanceof IOException) {
            LOGGER.log(Level.SEVERE, "I/O error", t);
            return new Result(503, "Service temporarily unavailable");
        }
        if (t.getClass().getName().contains("mapdb") || t.getCause() != null && t.getCause().getClass().getName().contains("mapdb")) {
            LOGGER.log(Level.SEVERE, "Error de base de datos MapDB", t);
            return new Result(503, "Service temporarily unavailable");
        }
        LOGGER.log(Level.SEVERE, "Error interno no clasificado", t);
        return new Result(500, "Internal server error");
    }
}
