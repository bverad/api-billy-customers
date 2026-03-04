package com.billy.customers.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSupport() {
    }

    static <T> T readJson(InputStream in, Class<T> type) throws IOException {
        return MAPPER.readValue(in, type);
    }

    static byte[] writeJsonBytes(Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(value);
    }

    static void writeJsonResponse(OutputStream out, Object value) throws IOException {
        out.write(writeJsonBytes(value));
    }
}

