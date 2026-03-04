package com.billy.customers;

import com.billy.customers.common.RateLimiter;
import com.billy.customers.config.SeedData;
import com.billy.customers.http.CustomersHandler;
import com.billy.customers.http.ItemsHandler;
import com.billy.customers.http.NotFoundHandler;
import com.billy.customers.persistence.MapDbDatabase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración de la API exponiendo los endpoints HTTP reales.
 */
public class ApiIntegrationTest {

    private static HttpServer server;
    private static ExecutorService executor;
    private static MapDbDatabase database;
    private static URI baseUri;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void startServer() throws IOException {
        Path dbPath = Path.of("billy-api-customers-test.db");
        Files.deleteIfExists(dbPath);

        database = MapDbDatabase.openFileDb(dbPath.toString());
        SeedData.seedIfEmpty(database);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);

        RateLimiter rateLimiter = new RateLimiter(500); // límite alto para pruebas
        server.createContext("/customers", new CustomersHandler(database, rateLimiter));
        server.createContext("/items", new ItemsHandler(database, rateLimiter));
        server.createContext("/", new NotFoundHandler(rateLimiter));

        server.start();

        int port = server.getAddress().getPort();
        baseUri = URI.create("http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() throws IOException {
        if (server != null) {
            server.stop(1);
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (database != null) {
            database.close();
        }
        Files.deleteIfExists(Path.of("billy-api-customers-test.db"));
    }

    @Test
    void customersCrudEndToEnd() throws Exception {
        // Listar clientes iniciales (semilla)
        HttpResponse<String> listResponse = sendGet("/customers");
        assertEquals(200, listResponse.statusCode());
        List<Map<String, Object>> initial = MAPPER.readValue(
                listResponse.body(), new TypeReference<>() {});
        assertTrue(initial.size() >= 1);

        // Crear un nuevo cliente
        String newCustomerJson = """
                {
                  "name": "Test",
                  "lastname": "User",
                  "gender": "M",
                  "email": "test.user@example.com"
                }
                """;
        HttpResponse<String> createResponse = sendPost("/customers", newCustomerJson);
        assertEquals(201, createResponse.statusCode());
        Map<String, Object> created = MAPPER.readValue(createResponse.body(), new TypeReference<>() {});
        String id = (String) created.get("id");
        assertNotNull(id);
        assertEquals("Test", created.get("name"));

        // Obtener por id
        HttpResponse<String> getResponse = sendGet("/customers/" + id);
        assertEquals(200, getResponse.statusCode());

        // Actualizar parcialmente (PATCH)
        String patchJson = """
                {
                  "email": "updated.user@example.com"
                }
                """;
        HttpResponse<String> patchResponse = sendPatch("/customers/" + id, patchJson);
        assertEquals(200, patchResponse.statusCode());
        Map<String, Object> patched = MAPPER.readValue(patchResponse.body(), new TypeReference<>() {});
        assertEquals("updated.user@example.com", patched.get("email"));

        // Eliminar
        HttpResponse<String> deleteResponse = sendDelete("/customers/" + id);
        assertEquals(204, deleteResponse.statusCode());

        // Verificar 404 tras eliminar
        HttpResponse<String> getAfterDelete = sendGet("/customers/" + id);
        assertEquals(404, getAfterDelete.statusCode());
    }

    @Test
    void itemsCrudEndToEnd() throws Exception {
        // Listar ítems iniciales (semilla)
        HttpResponse<String> listResponse = sendGet("/items");
        assertEquals(200, listResponse.statusCode());
        List<Map<String, Object>> initial = MAPPER.readValue(
                listResponse.body(), new TypeReference<>() {});
        assertTrue(initial.size() >= 1);

        // Crear un ítem
        String newItemJson = """
                {
                  "name": "Monitor",
                  "size": "24 pulgadas",
                  "weight": "4",
                  "color": "negro"
                }
                """;
        HttpResponse<String> createResponse = sendPost("/items", newItemJson);
        assertEquals(201, createResponse.statusCode());
        Map<String, Object> created = MAPPER.readValue(createResponse.body(), new TypeReference<>() {});
        String id = (String) created.get("id");
        assertNotNull(id);
        assertEquals("Monitor", created.get("name"));
        assertEquals("negro", created.get("color"));

        // Obtener por id
        HttpResponse<String> getResponse = sendGet("/items/" + id);
        assertEquals(200, getResponse.statusCode());
        Map<String, Object> got = MAPPER.readValue(getResponse.body(), new TypeReference<>() {});
        assertEquals(id, got.get("id"));
        assertEquals("Monitor", got.get("name"));

        // Actualizar parcialmente (PATCH)
        String patchJson = """
                {
                  "color": "blanco",
                  "weight": "5"
                }
                """;
        HttpResponse<String> patchResponse = sendPatch("/items/" + id, patchJson);
        assertEquals(200, patchResponse.statusCode());
        Map<String, Object> patched = MAPPER.readValue(patchResponse.body(), new TypeReference<>() {});
        assertEquals("blanco", patched.get("color"));
        assertEquals("5", patched.get("weight"));
        assertEquals("Monitor", patched.get("name"));

        // Eliminar
        HttpResponse<String> deleteResponse = sendDelete("/items/" + id);
        assertEquals(204, deleteResponse.statusCode());

        // Verificar 404 tras eliminar
        HttpResponse<String> getAfterDelete = sendGet("/items/" + id);
        assertEquals(404, getAfterDelete.statusCode());
    }

    @Test
    void itemMissingNameReturns400() throws Exception {
        String invalidItemJson = """
                {
                  "size": "grande",
                  "weight": "1",
                  "color": "azul"
                }
                """;
        HttpResponse<String> response = sendPost("/items", invalidItemJson);
        assertEquals(400, response.statusCode());
    }

    @Test
    void itemNotFoundReturns404() throws Exception {
        HttpResponse<String> response = sendGet("/items/id-inexistente-12345");
        assertEquals(404, response.statusCode());
    }

    @Test
    void unknownEndpointReturns404Json() throws Exception {
        HttpResponse<String> response = sendGet("/ruta-inexistente");
        assertEquals(404, response.statusCode());
        Map<String, Object> body = MAPPER.readValue(response.body(), new TypeReference<>() {});
        assertEquals("Not found", body.get("message"));
    }

    @Test
    void invalidCustomerEmailReturns400() throws Exception {
        String invalidCustomerJson = """
                {
                  "name": "Bad",
                  "lastname": "Email",
                  "gender": "M",
                  "email": "no-es-un-email"
                }
                """;
        HttpResponse<String> response = sendPost("/customers", invalidCustomerJson);
        assertEquals(400, response.statusCode());
    }

    // ---- helpers HTTP ----

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPatch(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendDelete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .DELETE()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

