package com.billy.customers.config;

import com.billy.customers.domain.Customer;
import com.billy.customers.domain.Item;
import com.billy.customers.persistence.MapDbDatabase;

/**
 * Inserta datos iniciales por defecto en la base de datos cuando las colecciones están vacías.
 */
public final class SeedData {

    private SeedData() {
    }

    /**
     * Si no hay clientes ni ítems, inserta unos pocos registros de ejemplo.
     */
    public static void seedIfEmpty(MapDbDatabase database) {
        boolean customersEmpty = database.customers().isEmpty();
        boolean itemsEmpty = database.items().isEmpty();

        if (customersEmpty) {
            database.customers().put("cust-001", new Customer(
                    "cust-001",
                    "Ana",
                    "García",
                    "F",
                    "ana.garcia@example.com"
            ));
            database.customers().put("cust-002", new Customer(
                    "cust-002",
                    "Luis",
                    "Martínez",
                    "M",
                    "luis.martinez@example.com"
            ));
            database.customers().put("cust-003", new Customer(
                    "cust-003",
                    "María",
                    "López",
                    "F",
                    "maria.lopez@example.com"
            ));
        }

        if (itemsEmpty) {
            database.items().put("item-001", new Item(
                    "item-001",
                    "Silla ergonómica",
                    "65x60x120 cm",
                    "12",
                    "negro"
            ));
            database.items().put("item-002", new Item(
                    "item-002",
                    "Escritorio",
                    "120x60x75 cm",
                    "25",
                    "blanco"
            ));
            database.items().put("item-003", new Item(
                    "item-003",
                    "Lámpara LED",
                    "20x20x45 cm",
                    "1",
                    "gris"
            ));
        }

        if (customersEmpty || itemsEmpty) {
            database.commit();
        }
    }
}
