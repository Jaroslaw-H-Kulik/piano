package com.piano.customers.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * Failures raised by the web container or the framework, before any controller runs, still use the
 * error shape.
 */
class ErrorShapeApiIT extends ApiTest {

    @Test
    void rejectsARequestWithoutTheTenantHeader() {
        assertInvalidRequest(
                clientWithoutTenantHeader().getCustomer("cus_123"),
                "Header X-Tenant-Id is required");
    }

    @Test
    void rejectsABlankTenantHeader() {
        assertInvalidRequest(
                clientWithTenantHeader("  ").getCustomer("cus_123"),
                "Tenant id must be 1-128 characters: letters, digits, underscore or hyphen");
    }

    @Test
    void answersAnUnknownPath() {
        assertError(
                tenantA.send(HttpMethod.GET, "/nothing", null, null),
                404,
                "NOT_FOUND",
                "No such endpoint");
    }

    @Test
    void answersAWrongMethod() {
        ApiResponse response =
                tenantA.send(
                        HttpMethod.PUT, "/customers/cus_123", MediaType.APPLICATION_JSON, "{}");

        assertError(response, 405, "METHOD_NOT_ALLOWED", "Method PUT is not supported here");
    }

    @Test
    void answersAWrongContentType() {
        ApiResponse response =
                tenantA.send(HttpMethod.POST, "/customers", MediaType.TEXT_PLAIN, "x");

        assertError(
                response, 415, "UNSUPPORTED_MEDIA_TYPE", "Content type must be application/json");
    }

    @Test
    void errorsAreJson() {
        ApiResponse response = tenantA.getCustomer("nobody");

        assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
