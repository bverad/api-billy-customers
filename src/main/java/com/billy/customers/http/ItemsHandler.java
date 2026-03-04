package com.billy.customers.http;

import com.billy.customers.common.InputValidator;
import com.billy.customers.common.RateLimiter;
import com.billy.customers.domain.Item;
import com.billy.customers.persistence.MapDbDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler HTTP para la colección items.
 */
public class ItemsHandler extends BaseHandler {

    private final MapDbDatabase database;

    public ItemsHandler(MapDbDatabase database, RateLimiter rateLimiter) {
        super(rateLimiter);
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!checkRateLimit(exchange)) return;
        logRequest(exchange);
        try {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath(); // /items o /items/{id}
            String[] segments = path.split("/");

            if (segments.length == 2) {
                // /items
                switch (method) {
                    case "GET" -> handleList(exchange);
                    case "POST" -> handleCreate(exchange);
                    default -> sendError(exchange, 405, "Method not allowed for /items");
                }
            } else if (segments.length == 3) {
                // /items/{id}
                String id = segments[2];
                var idError = InputValidator.validateId(id);
                if (idError.isPresent()) {
                    sendError(exchange, 400, idError.get());
                    return;
                }
                switch (method) {
                    case "GET" -> handleGetById(exchange, id);
                    case "PATCH" -> handleUpdate(exchange, id);
                    case "DELETE" -> handleDelete(exchange, id);
                    default -> sendError(exchange, 405, "Method not allowed for /items/{id}");
                }
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            handleException(exchange, e);
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        List<Item> all = new ArrayList<>(database.items().values());
        sendJson(exchange, 200, all);
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        Item item = database.items().get(id);
        if (item == null) {
            sendError(exchange, 404, "Item not found");
            return;
        }
        sendJson(exchange, 200, item);
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        if (!checkBodySizeLimit(exchange)) return;
        Item incoming;
        try {
            incoming = JsonSupport.readJson(exchange.getRequestBody(), Item.class);
        } catch (JsonProcessingException e) {
            sendError(exchange, 400, "cuerpo JSON inválido");
            return;
        }

        var validationError = InputValidator.validateItemForCreate(incoming);
        if (validationError.isPresent()) {
            sendError(exchange, 400, validationError.get());
            return;
        }

        String id = incoming.id() != null && !incoming.id().isBlank()
                ? incoming.id().trim()
                : UUID.randomUUID().toString();

        Item item = InputValidator.sanitizeItem(
                id,
                incoming.name(),
                incoming.size(),
                incoming.weight(),
                incoming.color()
        );

        database.items().put(id, item);
        database.commit();

        sendJson(exchange, 201, item);
    }

    @SuppressWarnings("unchecked")
    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        Item existing = database.items().get(id);
        if (existing == null) {
            sendError(exchange, 404, "Item not found");
            return;
        }

        if (!checkBodySizeLimit(exchange)) return;
        Map<String, Object> patch;
        try {
            patch = JsonSupport.readJson(exchange.getRequestBody(), Map.class);
        } catch (JsonProcessingException e) {
            sendError(exchange, 400, "cuerpo JSON inválido");
            return;
        }

        String name = patch.containsKey("name") ? (String) patch.get("name") : existing.name();
        String size = patch.containsKey("size") ? (String) patch.get("size") : existing.size();
        String weight = patch.containsKey("weight") ? (String) patch.get("weight") : existing.weight();
        String color = patch.containsKey("color") ? (String) patch.get("color") : existing.color();

        var validationError = InputValidator.validateItemPatch(name, size, weight, color);
        if (validationError.isPresent()) {
            sendError(exchange, 400, validationError.get());
            return;
        }

        Item updated = InputValidator.sanitizeItem(id, name, size, weight, color);

        database.items().put(id, updated);
        database.commit();

        sendJson(exchange, 200, updated);
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        Item removed = database.items().remove(id);
        if (removed == null) {
            sendError(exchange, 404, "Item not found");
            return;
        }
        database.commit();
        sendNoContent(exchange);
    }
}

