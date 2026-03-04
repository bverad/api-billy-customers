package com.billy.customers.persistence;

import com.billy.customers.domain.Customer;
import com.billy.customers.domain.Item;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de acceso a la base de datos embebida MapDB.
 */
public final class MapDbDatabase implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(MapDbDatabase.class.getName());

    private final DB db;
    private final ConcurrentMap<String, Customer> customers;
    private final ConcurrentMap<String, Item> items;

    private MapDbDatabase(DB db,
                          ConcurrentMap<String, Customer> customers,
                          ConcurrentMap<String, Item> items) {
        this.db = db;
        this.customers = customers;
        this.items = items;
    }

    /**
     * Abre (o crea) un archivo de base de datos MapDB en disco.
     */
    public static MapDbDatabase openFileDb(String fileName) {
        LOGGER.info(() -> "Abriendo base de datos embebida en " + fileName);
        DB db = DBMaker
                .fileDB(fileName)
                .transactionEnable()
                .make();

        ConcurrentMap<String, Customer> customers = db
                .hashMap("customers", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
        ConcurrentMap<String, Item> items = db
                .hashMap("items", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();

        return new MapDbDatabase(db, customers, items);
    }

    public ConcurrentMap<String, Customer> customers() {
        return customers;
    }

    public ConcurrentMap<String, Item> items() {
        return items;
    }

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 50;

    /**
     * Persiste cambios con reintentos y backoff exponencial ante fallos transitorios.
     */
    public void commit() {
        long backoffMs = INITIAL_BACKOFF_MS;
        Exception last = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                db.commit();
                return;
            } catch (Exception e) {
                last = e;
                LOGGER.log(Level.WARNING, "Commit MapDB intento " + (attempt + 1) + "/" + MAX_RETRIES, e);
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Commit interrumpido", ie);
                    }
                    backoffMs *= 2;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Commit MapDB falló tras " + MAX_RETRIES + " intentos", last);
        throw last instanceof RuntimeException r ? r : new RuntimeException(last);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Cerrando base de datos embebida MapDB");
        db.close();
    }
}

