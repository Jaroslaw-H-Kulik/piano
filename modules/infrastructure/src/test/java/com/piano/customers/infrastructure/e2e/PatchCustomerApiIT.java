package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code PATCH /customers/{id}}: for every attribute, omitted = keep, null = clear, value = set.
 */
class PatchCustomerApiIT extends CustomerApiTest {

    @Test
    void anEmptyPatchChangesNothing() {
        givenJohnSmith(tenantA);

        assertCustomer(tenantA.patchCustomer(CUSTOMER_ID, json()), johnSmith());
        assertStored(TENANT_A, johnSmith());
    }

    @Test
    void aFetchedCustomerCanBePatchedBackAsIs() {
        givenJohnSmith(tenantA);

        assertCustomer(tenantA.patchCustomer(CUSTOMER_ID, johnSmith()), johnSmith());
        assertStored(TENANT_A, johnSmith());
    }

    @Test
    void patchSetsKeepsAndClearsEachAttributeIndependently() {
        givenJohnSmith(tenantA);
        Map<String, Object> expected =
                customer(CUSTOMER_ID, null, "Smith", json("dob", DOB, "score", 7));

        ApiResponse response =
                tenantA.patchCustomer(
                        CUSTOMER_ID,
                        json("first_name", null, "generic_fields", json("score", 7, "vip", null)));

        assertCustomer(response, expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void changesTheLastNameAlone() {
        givenJohnSmith(tenantA);
        Map<String, Object> expected = customer(CUSTOMER_ID, "John", "Bull", johnSmithFields());

        assertCustomer(tenantA.patchCustomer(CUSTOMER_ID, json("last_name", "Bull")), expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void clearsADatetimeField() {
        givenJohnSmith(tenantA);
        Map<String, Object> expected =
                customer(CUSTOMER_ID, "John", "Smith", json("vip", true, "score", 100));

        ApiResponse response =
                tenantA.patchCustomer(CUSTOMER_ID, json("generic_fields", json("dob", null)));

        assertCustomer(response, expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void canAddAFieldThatWasNeverSet() {
        registerTaskExampleFields(tenantA);
        givenCustomer(tenantA, json("id", CUSTOMER_ID));
        Map<String, Object> expected = customer(CUSTOMER_ID, null, null, json("vip", true));

        assertCustomer(
                tenantA.patchCustomer(CUSTOMER_ID, json("generic_fields", json("vip", true))),
                expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void clearingAFieldThatWasNeverSetIsNotAnError() {
        registerTaskExampleFields(tenantA);
        givenCustomer(tenantA, json("id", CUSTOMER_ID));
        Map<String, Object> expected = customer(CUSTOMER_ID, null, null, Map.of());

        assertCustomer(
                tenantA.patchCustomer(CUSTOMER_ID, json("generic_fields", json("vip", null))),
                expected);
        assertStored(TENANT_A, expected);
    }

    @Test
    void answersNotFoundForAMissingCustomer() {
        assertCustomerNotFound(
                tenantA.patchCustomer(UNKNOWN_CUSTOMER_ID, json("first_name", "X")),
                UNKNOWN_CUSTOMER_ID);
    }

    @Test
    void rejectsAnIdThatCanNeverExist() {
        assertInvalidRequest(
                tenantA.patchCustomer("not.valid", json("first_name", "X")),
                INVALID_CUSTOMER_ID_MESSAGE);
    }

    @Test
    void doesNotChangeACustomerOfAnotherTenant() {
        givenJohnSmith(tenantA);

        assertCustomerNotFound(
                tenantB.patchCustomer(CUSTOMER_ID, json("first_name", "X")), CUSTOMER_ID);

        assertStored(TENANT_A, johnSmith());
        assertNotStored(TENANT_B, CUSTOMER_ID);
    }

    @Test
    void validatesFieldsAgainstTheTenantsOwnDefinitions() {
        tenantA.registerField("level", "INTEGER");
        tenantB.registerField("level", "STRING");
        givenCustomer(tenantB, json("id", CUSTOMER_ID));

        assertInvalidRequest(
                tenantB.patchCustomer(CUSTOMER_ID, json("generic_fields", json("level", 3))),
                "Field level expects STRING but received an integer");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPatchRequests")
    void rejectsAnInvalidPatchAndLeavesTheCustomerUnchanged(
            String description, Map<String, Object> body, String message) {
        givenJohnSmith(tenantA);

        assertInvalidRequest(tenantA.patchCustomer(CUSTOMER_ID, body), message);

        assertStored(TENANT_A, johnSmith());
    }

    static Stream<Arguments> invalidPatchRequests() {
        return Stream.of(
                Arguments.of("changing the id", json("id", "cus_999"), ID_CANNOT_CHANGE_MESSAGE),
                Arguments.of("id that is not a string", json("id", 123), ID_CANNOT_CHANGE_MESSAGE),
                Arguments.of(
                        "unknown property", json("tenant_id", "x"), "Unknown property: tenant_id"),
                Arguments.of(
                        "clearing an unregistered field",
                        json("generic_fields", json("shoe_size", null)),
                        "Generic field shoe_size is not registered for this tenant"),
                Arguments.of(
                        "wrong type for a field",
                        json("generic_fields", json("score", "high")),
                        "Field score expects INTEGER but received a string"),
                Arguments.of(
                        "name is not a string",
                        json("first_name", 5),
                        "Property first_name must be a string or null"),
                Arguments.of(
                        "a valid change next to an invalid one",
                        json("first_name", "Lewis", "generic_fields", json("score", "high")),
                        "Field score expects INTEGER but received a string"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedBodies")
    void rejectsAMalformedBodyAndLeavesTheCustomerUnchanged(String description, String rawBody) {
        givenJohnSmith(tenantA);

        assertInvalidRequest(
                tenantA.patchCustomerRaw(CUSTOMER_ID, rawBody), MALFORMED_BODY_MESSAGE);

        assertStored(TENANT_A, johnSmith());
    }

    static Stream<Arguments> malformedBodies() {
        return Stream.of(
                Arguments.of("duplicate property", "{\"first_name\":\"A\",\"first_name\":\"B\"}"),
                Arguments.of("malformed JSON", "{not json"),
                Arguments.of("body that is not an object", "[1,2]"));
    }

    private Map<String, Object> johnSmithFields() {
        return json("dob", DOB, "vip", true, "score", 100);
    }
}
