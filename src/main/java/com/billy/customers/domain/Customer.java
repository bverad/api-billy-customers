package com.billy.customers.domain;

import java.io.Serializable;

public record Customer(
        String id,
        String name,
        String lastname,
        String gender,
        String email
) implements Serializable {
}

