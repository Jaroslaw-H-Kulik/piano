package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class CreateCustomerApiIT extends CustomerApiTest {

    @Test
    void createsACustomerWithOnlyAnId() {
        Map<String, Object> expected = customer(CUSTOMER_ID, null, null, Map.of());

        assertCustomer(tenantA.createCustomer(json("id", CUSTOMER_ID)), expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void storesTheExtremesOfEachFieldType() {
        tenantA.registerField("big", "INTEGER");
        tenantA.registerField("when", "DATETIME");
        tenantA.registerField("text", "STRING");
        Map<String, Object> fields =
                json("big", Long.MAX_VALUE, "when", "2024-02-29T23:59:59", "text", "");

        Map<String, Object> expected = customer(CUSTOMER_ID, null, null, fields);

        assertCustomer(
                tenantA.createCustomer(json("id", CUSTOMER_ID, "generic_fields", fields)),
                expected);
        assertCustomer(tenantA.getCustomer(CUSTOMER_ID), expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void storesTheTaskExampleCustomer() {
        registerTaskExampleFields(tenantA);

        assertCustomer(tenantA.createCustomer(johnSmith()), johnSmith());
        assertStored(TENANT_A, johnSmith());
    }

    @Test
    void acceptsAnIdOfTheMaximumLength() {
        String longestId = "a".repeat(128);
        Map<String, Object> expected = customer(longestId, null, null, Map.of());

        assertCustomer(tenantA.createCustomer(json("id", longestId)), expected);
        assertCustomer(tenantA.getCustomer(longestId), expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void anIdCanBeReusedAfterDeletion() {
        givenCustomer(tenantA, json("id", CUSTOMER_ID, "first_name", "A"));
        tenantA.deleteCustomer(CUSTOMER_ID);
        Map<String, Object> expected = customer(CUSTOMER_ID, "B", null, Map.of());

        assertCustomer(
                tenantA.createCustomer(json("id", CUSTOMER_ID, "first_name", "B")), expected);
        assertStored(TENANT_A, expected);
        assertStoredCustomerCount(1);
    }

    @Test
    void rejectsADuplicateCustomerIdAndKeepsTheOriginal() {
        givenJohnSmith(tenantA);

        assertError(
                tenantA.createCustomer(json("id", CUSTOMER_ID, "first_name", "Impostor")),
                409,
                "CUSTOMER_ALREADY_EXISTS",
                "Customer " + CUSTOMER_ID + " already exists");

        assertStored(TENANT_A, johnSmith());
        assertStoredCustomerCount(1);
    }

    @Test
    void aRejectedCreateStoresNothing() {
        registerTaskExampleFields(tenantA);

        tenantA.createCustomer(json("id", CUSTOMER_ID, "generic_fields", json("score", "high")));

        assertCustomerNotFound(tenantA.getCustomer(CUSTOMER_ID), CUSTOMER_ID);
        assertStoredCustomerCount(0);
    }

    // --- tenant isolation ---

    @Test
    void theSameIdMayExistInTwoTenants() {
        givenJohnSmith(tenantA);
        Map<String, Object> expectedForB = customer(CUSTOMER_ID, null, null, Map.of());

        assertCustomer(tenantB.createCustomer(json("id", CUSTOMER_ID)), expectedForB);

        assertStored(TENANT_A, johnSmith());
        assertStored(TENANT_B, expectedForB);
    }

    @Test
    void aFieldRegisteredByOneTenantIsUnknownToAnother() {
        tenantA.registerField("vip", "BOOL");

        ApiResponse response =
                tenantB.createCustomer(
                        json("id", CUSTOMER_ID, "generic_fields", json("vip", true)));

        assertInvalidRequest(response, "Generic field vip is not registered for this tenant");
        assertNotStored(TENANT_B, CUSTOMER_ID);
    }

    @Test
    void sameFieldNameMayHaveADifferentTypePerTenant() {
        tenantA.registerField("level", "INTEGER");
        tenantB.registerField("level", "STRING");

        assertStatus(
                tenantA.createCustomer(json("id", "c", "generic_fields", json("level", 3))),
                HttpStatus.OK);
        assertStatus(
                tenantB.createCustomer(json("id", "c", "generic_fields", json("level", "gold"))),
                HttpStatus.OK);
        assertInvalidRequest(
                tenantB.createCustomer(json("id", "d", "generic_fields", json("level", 3))),
                "Field level expects STRING but received an integer");
    }

    // --- invalid requests ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateRequests")
    void rejectsAnInvalidCreateRequest(
            String description, Map<String, Object> body, String message) {
        registerTaskExampleFields(tenantA);

        assertInvalidRequest(tenantA.createCustomer(body), message);

        assertStoredCustomerCount(0);
    }

    static Stream<Arguments> invalidCreateRequests() {
        return Stream.of(
                Arguments.of(
                        "unregistered field",
                        json("id", "c", "generic_fields", json("shoe_size", 42)),
                        "Generic field shoe_size is not registered for this tenant"),
                Arguments.of(
                        "string where integer expected",
                        json("id", "c", "generic_fields", json("score", "high")),
                        "Field score expects INTEGER but received a string"),
                Arguments.of(
                        "string where bool expected",
                        json("id", "c", "generic_fields", json("vip", "true")),
                        "Field vip expects BOOL but received a string"),
                Arguments.of(
                        "decimal where integer expected",
                        json("id", "c", "generic_fields", json("score", 1.5)),
                        "Field score must be a boolean, an integer, a string or null"),
                Arguments.of(
                        "integer beyond long",
                        json(
                                "id",
                                "c",
                                "generic_fields",
                                json("score", new BigInteger("9223372036854775808"))),
                        "Field score must be an integer between -9223372036854775808 and 9223372036854775807"),
                Arguments.of(
                        "datetime that is only a date",
                        json("id", "c", "generic_fields", json("dob", "1990-01-01")),
                        "Field dob expects a DATETIME in ISO 8601, e.g. 1990-01-01T00:00:00 or 1990-01-01T00:00:00Z"),
                Arguments.of(
                        "null generic field",
                        json("id", "c", "generic_fields", json("dob", null)),
                        "Field dob must not be null when creating a customer; omit it instead"),
                Arguments.of(
                        "missing id",
                        json("first_name", "John"),
                        "Property id is required and must be a string"),
                Arguments.of("empty id", json("id", ""), INVALID_CUSTOMER_ID_MESSAGE),
                Arguments.of("id with a slash", json("id", "cus/1"), INVALID_CUSTOMER_ID_MESSAGE),
                Arguments.of(
                        "id one character too long",
                        json("id", "a".repeat(129)),
                        INVALID_CUSTOMER_ID_MESSAGE),
                Arguments.of(
                        "id is not a string", json("id", 123), "Property id has the wrong type"),
                Arguments.of(
                        "first name is not a string",
                        json("id", "c", "first_name", true),
                        "Property first_name has the wrong type"),
                Arguments.of(
                        "generic fields is an array",
                        json("id", "c", "generic_fields", List.of(1)),
                        "Property generic_fields has the wrong type"),
                Arguments.of(
                        "tenant id in the body",
                        json("id", "c", "tenant_id", "tenant_123"),
                        "Unknown property: tenant_id"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedBodies")
    void rejectsAMalformedBody(String description, String rawBody) {
        assertInvalidRequest(tenantA.postRaw("/customers", rawBody), MALFORMED_BODY_MESSAGE);

        assertStoredCustomerCount(0);
    }

    static Stream<Arguments> malformedBodies() {
        return Stream.of(
                Arguments.of(
                        "duplicate property",
                        "{\"id\":\"cus_1\",\"first_name\":\"A\",\"first_name\":\"B\"}"),
                Arguments.of("malformed JSON", "{not json"),
                Arguments.of("body that is not an object", "[1,2]"));
    }
}
