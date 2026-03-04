package com.billy.customers.http;

import com.billy.customers.common.InputValidator;
import com.billy.customers.common.RateLimiter;
import com.billy.customers.domain.Customer;
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
 * Handler HTTP para la colección customers.
 */
public class CustomersHandler extends BaseHandler {

    private final MapDbDatabase database;

    public CustomersHandler(MapDbDatabase database, RateLimiter rateLimiter) {
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
            String path = uri.getPath(); // /customers o /customers/{id}
            String[] segments = path.split("/");

            if (segments.length == 2) {
                // /customers
                switch (method) {
                    case "GET" -> handleList(exchange);
                    case "POST" -> handleCreate(exchange);
                    default -> sendError(exchange, 405, "Method not allowed for /customers");
                }
            } else if (segments.length == 3) {
                // /customers/{id}
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
                    default -> sendError(exchange, 405, "Method not allowed for /customers/{id}");
                }
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            handleException(exchange, e);
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        List<Customer> all = new ArrayList<>(database.customers().values());
        sendJson(exchange, 200, all);
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        Customer customer = database.customers().get(id);
        if (customer == null) {
            sendError(exchange, 404, "Customer not found");
            return;
        }
        sendJson(exchange, 200, customer);
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        if (!checkBodySizeLimit(exchange)) return;
        Customer incoming;
        try {
            incoming = JsonSupport.readJson(exchange.getRequestBody(), Customer.class);
        } catch (JsonProcessingException e) {
            sendError(exchange, 400, "cuerpo JSON inválido");
            return;
        }

        var validationError = InputValidator.validateCustomerForCreate(incoming);
        if (validationError.isPresent()) {
            sendError(exchange, 400, validationError.get());
            return;
        }

        String id = incoming.id() != null && !incoming.id().isBlank()
                ? incoming.id().trim()
                : UUID.randomUUID().toString();

        Customer customer = InputValidator.sanitizeCustomer(
                id,
                incoming.name(),
                incoming.lastname(),
                incoming.gender(),
                incoming.email()
        );

        database.customers().put(id, customer);
        database.commit();

        sendJson(exchange, 201, customer);
    }

    @SuppressWarnings("unchecked")
    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        Customer existing = database.customers().get(id);
        if (existing == null) {
            sendError(exchange, 404, "Customer not found");
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
        String lastname = patch.containsKey("lastname") ? (String) patch.get("lastname") : existing.lastname();
        String gender = patch.containsKey("gender") ? (String) patch.get("gender") : existing.gender();
        String email = patch.containsKey("email") ? (String) patch.get("email") : existing.email();

        var validationError = InputValidator.validateCustomerPatch(name, lastname, gender, email);
        if (validationError.isPresent()) {
            sendError(exchange, 400, validationError.get());
            return;
        }

        Customer updated = InputValidator.sanitizeCustomer(id, name, lastname, gender, email);

        database.customers().put(id, updated);
        database.commit();

        sendJson(exchange, 200, updated);
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        Customer removed = database.customers().remove(id);
        if (removed == null) {
            sendError(exchange, 404, "Customer not found");
            return;
        }
        database.commit();
        sendNoContent(exchange);
    }
}

