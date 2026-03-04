package com.billy.customers.domain;

import java.io.Serializable;

public record Item(
        String id,
        String name,
        String size,
        String weight,
        String color
) implements Serializable {
}

