package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GetCustomerApiIT extends CustomerApiTest {

    @Test
    void returnsTheCustomerWithAllItsFields() {
        givenJohnSmith(tenantA);

        assertCustomer(tenantA.getCustomer(CUSTOMER_ID), johnSmith());
    }

    @Test
    void returnsExplicitNullsAndAnEmptyMapForAnUnsetCustomer() {
        givenCustomer(tenantA, json("id", CUSTOMER_ID));

        assertCustomer(
                tenantA.getCustomer(CUSTOMER_ID), customer(CUSTOMER_ID, null, null, Map.of()));
    }

    @Test
    void answersNotFoundForAMissingCustomer() {
        assertCustomerNotFound(tenantA.getCustomer(UNKNOWN_CUSTOMER_ID), UNKNOWN_CUSTOMER_ID);
    }

    @Test
    void rejectsAnIdThatCanNeverExist() {
        assertInvalidRequest(tenantA.getCustomer("not.valid"), INVALID_CUSTOMER_ID_MESSAGE);
    }

    @Test
    void doesNotShowACustomerOfAnotherTenant() {
        givenJohnSmith(tenantA);

        assertCustomerNotFound(tenantB.getCustomer(CUSTOMER_ID), CUSTOMER_ID);
    }

    /**
     * A stored row that no longer passes validation is a server fault, not a bad request: the
     * client sent nothing wrong.
     */
    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            textBlock =
                    """
                    John | {"score": "100"}
                    John | {"dob": "yesterday"}
                    John | {"not a name": true}
                    ' '  | {}
                    John | {"unregistered": 1}
                    """)
    void answersAnInternalErrorForACorruptStoredCustomer(
            String firstName, String genericFieldsJson) {
        registerTaskExampleFields(tenantA);
        jdbc.sql(
                        """
                        INSERT INTO customer (tenant_id, public_id, first_name, generic_fields)
                        VALUES (?, ?, ?, CAST(? AS jsonb))""")
                .params(TENANT_A, CUSTOMER_ID, firstName, genericFieldsJson)
                .update();

        assertError(tenantA.getCustomer(CUSTOMER_ID), 500, "INTERNAL_ERROR", "Unexpected error");
    }
}
