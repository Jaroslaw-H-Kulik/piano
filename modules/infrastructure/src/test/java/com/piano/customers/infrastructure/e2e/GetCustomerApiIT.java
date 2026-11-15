package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
}
