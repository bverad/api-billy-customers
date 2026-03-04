package com.billy.customers.http;

import com.billy.customers.common.RateLimiter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Responde con 404 JSON para cualquier ruta no registrada en la API.
 * Evita la respuesta HTML por defecto del HttpServer.
 */
public class NotFoundHandler extends BaseHandler {

    public NotFoundHandler(RateLimiter rateLimiter) {
        super(rateLimiter);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!checkRateLimit(exchange)) return;
        sendError(exchange, 404, "Not found");
    }
}
