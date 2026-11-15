package com.piano.customers.infrastructure.e2e.helper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * A black-box HTTP client for one tenant. Requests are plain maps and responses are parsed into
 * plain maps, so the tests state the wire format (snake_case names, null vs. absent) without
 * sharing any class with the production code. Use {@link #send} with a raw string for bodies that a
 * map cannot express (malformed JSON, duplicate keys).
 */
public final class ApiClient {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final RestClient http;
    private final @Nullable String tenant;

    public ApiClient(int port, @Nullable String tenant) {
        this.tenant = tenant;
        this.http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        // Error statuses are results to assert on, not exceptions.
                        .defaultStatusHandler(status -> true, (request, response) -> {})
                        .build();
    }

    public ApiResponse registerField(String name, String type) {
        return post("/generic_fields", Map.of("name", name, "type", type));
    }

    public ApiResponse createCustomer(Map<String, ?> body) {
        return post("/customers", body);
    }

    public ApiResponse getCustomer(String id) {
        return send(HttpMethod.GET, "/customers/" + id, null, null);
    }

    public ApiResponse patchCustomer(String id, Map<String, ?> body) {
        return send(HttpMethod.PATCH, "/customers/" + id, MediaType.APPLICATION_JSON, body);
    }

    /** Sends a JSON string as it is, without going through a map. */
    public ApiResponse patchCustomerRaw(String id, String json) {
        return send(HttpMethod.PATCH, "/customers/" + id, MediaType.APPLICATION_JSON, json);
    }

    public ApiResponse deleteCustomer(String id) {
        return send(HttpMethod.DELETE, "/customers/" + id, null, null);
    }

    public ApiResponse post(String path, Object body) {
        return send(HttpMethod.POST, path, MediaType.APPLICATION_JSON, body);
    }

    /** Sends a JSON string as it is, without going through a map. */
    public ApiResponse postRaw(String path, String json) {
        return send(HttpMethod.POST, path, MediaType.APPLICATION_JSON, json);
    }

    public ApiResponse send(
            HttpMethod method,
            String path,
            @Nullable MediaType contentType,
            @Nullable Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path);
        if (tenant != null) {
            request = request.header(TENANT_HEADER, tenant);
        }
        if (contentType != null) {
            request = request.contentType(contentType);
        }
        if (body != null) {
            request = request.body(body);
        }
        ResponseEntity<Map<String, Object>> response =
                request.retrieve()
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        return new ApiResponse(
                response.getStatusCode().value(), response.getHeaders(), response.getBody());
    }

    /**
     * A mutable map that accepts null values, which {@code Map.of} does not. Pairs are name, value,
     * name, value.
     */
    public static Map<String, Object> json(Object... namesAndValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            result.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return result;
    }
}
