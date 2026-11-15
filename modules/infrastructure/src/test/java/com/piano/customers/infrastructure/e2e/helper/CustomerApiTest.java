package com.piano.customers.infrastructure.e2e.helper;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base of the customer API tests: the task's example data, the "given" helpers that set up state
 * through the API and fail loudly when a setup call is rejected, and the assertions on customer
 * responses.
 */
public abstract class CustomerApiTest extends ApiTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    protected static final String CUSTOMER_ID = "cus_123";
    protected static final String OTHER_CUSTOMER_ID = "cus_456";
    protected static final String UNKNOWN_CUSTOMER_ID = "nobody";

    protected static final String DOB = "1990-01-01T00:00:00";

    protected static final String MALFORMED_BODY_MESSAGE =
            "The request body must be a valid JSON object without duplicate properties";
    protected static final String INVALID_CUSTOMER_ID_MESSAGE =
            "Customer id must be 1-128 characters: letters, digits, underscore or hyphen";
    protected static final String ID_CANNOT_CHANGE_MESSAGE =
            "The id of a customer cannot be changed";

    /**
     * Registers the fields used by the task's example: dob (DATETIME), vip (BOOL), score (INTEGER).
     */
    protected void registerTaskExampleFields(ApiClient client) {
        assertStatus(client.registerField("dob", "DATETIME"), HttpStatus.OK);
        assertStatus(client.registerField("vip", "BOOL"), HttpStatus.OK);
        assertStatus(client.registerField("score", "INTEGER"), HttpStatus.OK);
    }

    /**
     * Creates a customer for set-up purposes; a rejected request fails the test here, not in a
     * later assertion.
     */
    protected void givenCustomer(ApiClient client, Map<String, Object> body) {
        assertStatus(client.createCustomer(body), HttpStatus.OK);
    }

    /**
     * Registers the task's example fields and creates {@link #johnSmith()} for the given tenant.
     */
    protected void givenJohnSmith(ApiClient client) {
        registerTaskExampleFields(client);
        givenCustomer(client, johnSmith());
    }

    /**
     * The request body (and, as it is echoed back unchanged, the response body) of the task's
     * example customer.
     */
    protected Map<String, Object> johnSmith() {
        return json(
                "id",
                CUSTOMER_ID,
                "first_name",
                "John",
                "last_name",
                "Smith",
                "generic_fields",
                json("dob", DOB, "vip", true, "score", 100));
    }

    /** A full customer representation as the API returns it: absent names are explicit nulls. */
    protected static Map<String, Object> customer(
            String id, String firstName, String lastName, Map<String, Object> genericFields) {
        return json(
                "id",
                id,
                "first_name",
                firstName,
                "last_name",
                lastName,
                "generic_fields",
                genericFields);
    }

    /** Both create and update answer 200 with the full customer, as does get. */
    protected static void assertCustomer(ApiResponse response, Map<String, Object> expectedBody) {
        assertStatus(response, HttpStatus.OK);
        assertThat(response.body()).isEqualTo(expectedBody);
    }

    /**
     * Asserts on the row in PostgreSQL, not on what the API says. The row is read into the same
     * shape as the API representation, so the expected value is built with {@link #customer} like
     * in the response assertions.
     */
    protected void assertStored(String tenantId, Map<String, Object> expectedCustomer) {
        String id = (String) expectedCustomer.get("id");
        assertThat(readStoredCustomer(tenantId, id))
                .as("row of %s in %s", id, tenantId)
                .isEqualTo(expectedCustomer);
    }

    protected void assertNotStored(String tenantId, String id) {
        assertThat(readStoredCustomer(tenantId, id)).as("row of %s in %s", id, tenantId).isNull();
    }

    protected void assertStoredCustomerCount(long expected) {
        assertThat(jdbc.sql("SELECT count(*) FROM customer").query(Long.class).single())
                .isEqualTo(expected);
    }

    private @Nullable Map<String, Object> readStoredCustomer(String tenantId, String id) {
        return jdbc.sql(
                        """
                        SELECT first_name, last_name, generic_fields::text AS generic_fields
                        FROM customer WHERE tenant_id = ? AND public_id = ?""")
                .params(tenantId, id)
                .query(
                        (row, rowNumber) ->
                                customer(
                                        id,
                                        row.getString("first_name"),
                                        row.getString("last_name"),
                                        parseJsonObject(row.getString("generic_fields"))))
                .optional()
                .orElse(null);
    }

    private static Map<String, Object> parseJsonObject(String text) {
        return JSON_MAPPER.readValue(text, new TypeReference<Map<String, Object>>() {});
    }

    protected static void assertCustomerNotFound(ApiResponse response, String id) {
        assertError(response, 404, "CUSTOMER_NOT_FOUND", "Customer " + id + " was not found");
    }
}
