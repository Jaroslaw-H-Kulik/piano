package com.piano.customers.infrastructure.e2e.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.piano.customers.infrastructure.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

/** Base of the API tests: real HTTP against the embedded server, one client per tenant. */
public abstract class ApiTest extends IntegrationTest {

    protected static final String TENANT_A = "tenant_123";
    protected static final String TENANT_B = "tenant_456";

    @LocalServerPort private int port;

    protected ApiClient tenantA;
    protected ApiClient tenantB;

    @BeforeEach
    void createClients() {
        tenantA = new ApiClient(port, TENANT_A);
        tenantB = new ApiClient(port, TENANT_B);
    }

    protected ApiClient clientWithTenantHeader(String value) {
        return new ApiClient(port, value);
    }

    protected ApiClient clientWithoutTenantHeader() {
        return new ApiClient(port, null);
    }

    protected static void assertStatus(ApiResponse response, HttpStatus expected) {
        assertThat(response.status())
                .as("status of %s", response.body())
                .isEqualTo(expected.value());
    }

    /**
     * The whole error body is compared, so a wrong code, a wrong message or an extra property fails
     * the test.
     */
    protected static void assertError(
            ApiResponse response, int status, String code, String message) {
        assertThat(response.status()).as("status of %s", response.body()).isEqualTo(status);
        assertThat(response.body()).isEqualTo(Map.of("error", code, "message", message));
    }

    protected static void assertInvalidRequest(ApiResponse response, String message) {
        assertError(response, 400, "INVALID_REQUEST", message);
    }
}
