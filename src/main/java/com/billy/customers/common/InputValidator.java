package com.billy.customers.common;

import com.billy.customers.domain.Customer;
import com.billy.customers.domain.Item;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validación y sanitización de entrada para la API.
 * Rechazar con 400 cuando los datos sean inválidos; sanitizar (trim, límites) antes de persistir.
 */
public final class InputValidator {

    private static final int ID_MAX_LENGTH = 64;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private static final int NAME_MAX_LENGTH = 200;
    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int GENDER_MAX_LENGTH = 20;
    private static final int FIELD_MAX_LENGTH = 200;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    private InputValidator() {
    }

    /**
     * Valida el id usado en path (evitar inyección, caracteres no permitidos).
     */
    public static Optional<String> validateId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.of("id: no puede estar vacío");
        }
        if (id.length() > ID_MAX_LENGTH) {
            return Optional.of("id: longitud máxima " + ID_MAX_LENGTH);
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            return Optional.of("id: solo se permiten letras, números, guión y guión bajo");
        }
        return Optional.empty();
    }

    /**
     * Valida y devuelve mensaje de error si el cliente es inválido para creación.
     */
    public static Optional<String> validateCustomerForCreate(Customer c) {
        if (c == null) {
            return Optional.of("cuerpo JSON inválido o vacío");
        }
        if (c.name() == null || c.name().isBlank()) {
            return Optional.of("name: es obligatorio");
        }
        if (c.name().length() > NAME_MAX_LENGTH) {
            return Optional.of("name: longitud máxima " + NAME_MAX_LENGTH);
        }
        if (c.lastname() == null || c.lastname().isBlank()) {
            return Optional.of("lastname: es obligatorio");
        }
        if (c.lastname().length() > NAME_MAX_LENGTH) {
            return Optional.of("lastname: longitud máxima " + NAME_MAX_LENGTH);
        }
        if (c.email() == null || c.email().isBlank()) {
            return Optional.of("email: es obligatorio");
        }
        if (c.email().length() > EMAIL_MAX_LENGTH) {
            return Optional.of("email: longitud máxima " + EMAIL_MAX_LENGTH);
        }
        if (!EMAIL_PATTERN.matcher(c.email().trim()).matches()) {
            return Optional.of("email: formato inválido");
        }
        if (c.gender() != null && c.gender().length() > GENDER_MAX_LENGTH) {
            return Optional.of("gender: longitud máxima " + GENDER_MAX_LENGTH);
        }
        if (c.id() != null && !c.id().isBlank()) {
            return validateId(c.id()).map(msg -> "id: " + msg.replace("id: ", ""));
        }
        return Optional.empty();
    }

    /**
     * Valida payload PATCH para cliente: valores no nulos deben ser válidos.
     */
    public static Optional<String> validateCustomerPatch(String name, String lastname, String gender, String email) {
        if (name != null && name.length() > NAME_MAX_LENGTH) {
            return Optional.of("name: longitud máxima " + NAME_MAX_LENGTH);
        }
        if (lastname != null && lastname.length() > NAME_MAX_LENGTH) {
            return Optional.of("lastname: longitud máxima " + NAME_MAX_LENGTH);
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return Optional.of("email: formato inválido");
        }
        if (email != null && email.length() > EMAIL_MAX_LENGTH) {
            return Optional.of("email: longitud máxima " + EMAIL_MAX_LENGTH);
        }
        if (gender != null && gender.length() > GENDER_MAX_LENGTH) {
            return Optional.of("gender: longitud máxima " + GENDER_MAX_LENGTH);
        }
        return Optional.empty();
    }

    /**
     * Sanitiza y devuelve un Customer con campos recortados y límites aplicados.
     */
    public static Customer sanitizeCustomer(String id, String name, String lastname, String gender, String email) {
        return new Customer(
                id,
                trimAndLimit(name, NAME_MAX_LENGTH),
                trimAndLimit(lastname, NAME_MAX_LENGTH),
                trimAndLimit(gender, GENDER_MAX_LENGTH),
                trimAndLimit(email, EMAIL_MAX_LENGTH)
        );
    }

    /**
     * Valida ítem para creación.
     */
    public static Optional<String> validateItemForCreate(Item item) {
        if (item == null) {
            return Optional.of("cuerpo JSON inválido o vacío");
        }
        if (item.name() == null || item.name().isBlank()) {
            return Optional.of("name: es obligatorio");
        }
        if (item.name().length() > FIELD_MAX_LENGTH) {
            return Optional.of("name: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (item.size() != null && item.size().length() > FIELD_MAX_LENGTH) {
            return Optional.of("size: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (item.weight() != null && item.weight().length() > FIELD_MAX_LENGTH) {
            return Optional.of("weight: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (item.color() != null && item.color().length() > FIELD_MAX_LENGTH) {
            return Optional.of("color: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (item.id() != null && !item.id().isBlank()) {
            return validateId(item.id()).map(msg -> "id: " + msg.replace("id: ", ""));
        }
        return Optional.empty();
    }

    /**
     * Valida campos PATCH para ítem.
     */
    public static Optional<String> validateItemPatch(String name, String size, String weight, String color) {
        if (name != null && name.length() > FIELD_MAX_LENGTH) {
            return Optional.of("name: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (size != null && size.length() > FIELD_MAX_LENGTH) {
            return Optional.of("size: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (weight != null && weight.length() > FIELD_MAX_LENGTH) {
            return Optional.of("weight: longitud máxima " + FIELD_MAX_LENGTH);
        }
        if (color != null && color.length() > FIELD_MAX_LENGTH) {
            return Optional.of("color: longitud máxima " + FIELD_MAX_LENGTH);
        }
        return Optional.empty();
    }

    /**
     * Sanitiza y devuelve un Item con campos recortados y límites aplicados.
     */
    public static Item sanitizeItem(String id, String name, String size, String weight, String color) {
        return new Item(
                id,
                trimAndLimit(name, FIELD_MAX_LENGTH),
                trimAndLimit(size, FIELD_MAX_LENGTH),
                trimAndLimit(weight, FIELD_MAX_LENGTH),
                trimAndLimit(color, FIELD_MAX_LENGTH)
        );
    }

    private static String trimAndLimit(String value, int maxLength) {
        if (value == null) return "";
        String t = value.trim();
        return t.length() > maxLength ? t.substring(0, maxLength) : t;
    }
}
