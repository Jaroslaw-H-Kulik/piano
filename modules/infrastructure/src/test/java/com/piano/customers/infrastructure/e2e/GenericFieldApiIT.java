package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.ApiTest;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GenericFieldApiIT extends ApiTest {

    @Test
    void registersAFieldAndEchoesIt() {
        ApiResponse response = tenantA.registerField("dob", "DATETIME");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(Map.of("name", "dob", "type", "DATETIME"));
    }

    @Test
    void storesTheFieldDefinitionForTheTenant() {
        tenantA.registerField("dob", "DATETIME");

        assertThat(
                        jdbc.sql(
                                        "SELECT type FROM generic_field_definition WHERE tenant_id = ? AND name = ?")
                                .params(TENANT_A, "dob")
                                .query(String.class)
                                .single())
                .isEqualTo("DATETIME");
        assertThat(
                        jdbc.sql("SELECT count(*) FROM generic_field_definition")
                                .query(Long.class)
                                .single())
                .isEqualTo(1);
    }

    @Test
    void rejectsRegisteringTheSameFieldTwice() {
        tenantA.registerField("dob", "DATETIME");

        assertError(
                tenantA.registerField("dob", "STRING"),
                409,
                "GENERIC_FIELD_ALREADY_EXISTS",
                "Generic field dob is already registered for this tenant");
    }

    @Test
    void allowsTheSameFieldNameInAnotherTenant() {
        tenantA.registerField("dob", "DATETIME");

        assertThat(tenantB.registerField("dob", "STRING").status()).isEqualTo(200);
    }

    @Test
    void fieldNamesAreCaseSensitive() {
        tenantA.registerField("dob", "DATETIME");

        assertThat(tenantA.registerField("DOB", "DATETIME").status()).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFieldRequests")
    void rejectsAnInvalidRegistration(
            String description, Map<String, Object> body, String message) {
        assertInvalidRequest(tenantA.post("/generic_fields", body), message);
    }

    static Stream<Arguments> invalidFieldRequests() {
        return Stream.of(
                Arguments.of(
                        "unknown type",
                        json("name", "dob", "type", "FLOAT"),
                        "Property type must be one of [BOOL, INTEGER, STRING, DATETIME]"),
                Arguments.of(
                        "type is case sensitive",
                        json("name", "dob", "type", "datetime"),
                        "Property type must be one of [BOOL, INTEGER, STRING, DATETIME]"),
                Arguments.of(
                        "missing name",
                        json("type", "BOOL"),
                        "Property name is required and must be a string"),
                Arguments.of(
                        "missing type",
                        json("name", "dob"),
                        "Property type is required and must be a string"),
                Arguments.of(
                        "type is not a string",
                        json("name", "dob", "type", 1),
                        "Property type has the wrong type"),
                Arguments.of(
                        "name is not a string",
                        json("name", 1, "type", "BOOL"),
                        "Property name has the wrong type"),
                Arguments.of(
                        "name with a space",
                        json("name", "has space", "type", "BOOL"),
                        "Field name must be 1-64 characters: letters, digits or underscore"),
                Arguments.of(
                        "name too long",
                        json("name", "a".repeat(65), "type", "BOOL"),
                        "Field name must be 1-64 characters: letters, digits or underscore"),
                Arguments.of(
                        "tenant id in the body",
                        json("name", "dob", "type", "BOOL", "tenant_id", "x"),
                        "Unknown property: tenant_id"));
    }

    @Test
    void rejectsADuplicateProperty() {
        ApiResponse response =
                tenantA.postRaw(
                        "/generic_fields", "{\"name\":\"a\",\"name\":\"b\",\"type\":\"BOOL\"}");

        assertInvalidRequest(
                response,
                "The request body must be a valid JSON object without duplicate properties");
    }
}
