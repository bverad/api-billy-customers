package com.billy.customers.config;

import com.billy.customers.common.RateLimiter;
import com.billy.customers.http.CustomersHandler;
import com.billy.customers.http.ItemsHandler;
import com.billy.customers.http.NotFoundHandler;
import com.billy.customers.persistence.MapDbDatabase;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la aplicación.
 *
 * Arranca el servidor HTTP embebido y prepara la base de datos embebida.
 */
public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) throws IOException {
        int port = 8080;

        MapDbDatabase database = MapDbDatabase.openFileDb("billy-api-customers.db");
        SeedData.seedIfEmpty(database);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);

        RateLimiter rateLimiter = new RateLimiter(200);
        server.createContext("/customers", new CustomersHandler(database, rateLimiter));
        server.createContext("/items", new ItemsHandler(database, rateLimiter));
        server.createContext("/", new NotFoundHandler(rateLimiter));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Apagando Billy API Customers...");
            try {
                // Dar un pequeño margen para que terminen las peticiones en curso
                server.stop(1);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al detener el servidor HTTP", e);
            }
            executor.shutdown();
            try {
                database.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la base de datos", e);
            }
        }));

        LOGGER.info("Billy API Customers escuchando en http://localhost:" + port);
        server.start();
    }
}

