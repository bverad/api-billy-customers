package com.billy.customers.http;

import com.billy.customers.common.ErrorMapping;
import com.billy.customers.common.RateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Funciones comunes para handlers HTTP:
 * rate limiting, serialización JSON, mapeo de errores y logging.
 */
public abstract class BaseHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(BaseHandler.class.getName());

    /** Límite de tamaño de body (1 MB) para evitar payloads gigantes. */
    private static final int MAX_BODY_SIZE_BYTES = 1024 * 1024;

    private final RateLimiter rateLimiter;

    protected BaseHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * Comprueba el límite de tamaño del body (Content-Length). Si se supera, envía 413 y devuelve false.
     * Llamar antes de leer el body en POST/PATCH.
     */
    protected boolean checkBodySizeLimit(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                long size = Long.parseLong(contentLength.trim());
                if (size > MAX_BODY_SIZE_BYTES) {
                    LOGGER.warning("Body supera límite: " + size + " bytes");
                    sendError(exchange, 413, "Payload too large");
                    return false;
                }
            } catch (NumberFormatException ignored) {
                // Content-Length inválido: dejar que la lectura falle después si hace falta
            }
        }
        return true;
    }

    /**
     * Comprueba el límite de peticiones; si se supera, envía 429 y devuelve false.
     */
    protected boolean checkRateLimit(HttpExchange exchange) throws IOException {
        if (!rateLimiter.allowRequest()) {
            LOGGER.warning("Rate limit superado");
            sendError(exchange, 429, "Too Many Requests");
            return false;
        }
        return true;
    }

    protected void logRequest(HttpExchange exchange) {
        LOGGER.info(() -> exchange.getRequestMethod() + " " + exchange.getRequestURI());
    }

    protected void logException(HttpExchange exchange, Exception e) {
        LOGGER.log(
                Level.SEVERE,
                "Error procesando " + exchange.getRequestMethod() + " " + exchange.getRequestURI(),
                e
        );
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes;
        try {
            bytes = JsonSupport.writeJsonBytes(body);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error serializando respuesta JSON", e);
            bytes = "{\"message\":\"internal serialization error\"}".getBytes(StandardCharsets.UTF_8);
            status = 500;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
        if (status >= 500) {
            LOGGER.severe(() -> "HTTP " + status + " - " + message);
        } else {
            LOGGER.warning(() -> "HTTP " + status + " - " + message);
        }
        sendJson(exchange, status, new ApiError(message));
    }

    /**
     * Mapea la excepción a código HTTP y mensaje (ErrorMapping) y envía la respuesta.
     */
    protected void handleException(HttpExchange exchange, Throwable t) throws IOException {
        ErrorMapping.Result r = ErrorMapping.from(t);
        sendError(exchange, r.status(), r.clientMessage());
    }
}

