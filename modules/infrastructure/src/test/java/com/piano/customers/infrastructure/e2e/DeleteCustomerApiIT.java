package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DeleteCustomerApiIT extends CustomerApiTest {

    @Test
    void deletesTheCustomer() {
        givenJohnSmith(tenantA);

        assertStatus(tenantA.deleteCustomer(CUSTOMER_ID), HttpStatus.NO_CONTENT);
        assertCustomerNotFound(tenantA.getCustomer(CUSTOMER_ID), CUSTOMER_ID);
        assertNotStored(TENANT_A, CUSTOMER_ID);
        assertStoredCustomerCount(0);
    }

    @Test
    void leavesOtherCustomersAlone() {
        givenCustomer(tenantA, json("id", CUSTOMER_ID));
        givenCustomer(tenantA, json("id", OTHER_CUSTOMER_ID));

        tenantA.deleteCustomer(CUSTOMER_ID);

        assertNotStored(TENANT_A, CUSTOMER_ID);
        assertStored(TENANT_A, customer(OTHER_CUSTOMER_ID, null, null, Map.of()));
    }

    @Test
    void deletingTwiceIsNotFoundTheSecondTime() {
        givenCustomer(tenantA, json("id", CUSTOMER_ID));
        tenantA.deleteCustomer(CUSTOMER_ID);

        assertCustomerNotFound(tenantA.deleteCustomer(CUSTOMER_ID), CUSTOMER_ID);
    }

    @Test
    void answersNotFoundForAMissingCustomer() {
        assertCustomerNotFound(tenantA.deleteCustomer(UNKNOWN_CUSTOMER_ID), UNKNOWN_CUSTOMER_ID);
    }

    @Test
    void rejectsAnIdThatCanNeverExist() {
        assertInvalidRequest(tenantA.deleteCustomer("not.valid"), INVALID_CUSTOMER_ID_MESSAGE);
    }

    @Test
    void doesNotDeleteACustomerOfAnotherTenant() {
        givenJohnSmith(tenantA);

        assertCustomerNotFound(tenantB.deleteCustomer(CUSTOMER_ID), CUSTOMER_ID);

        assertStored(TENANT_A, johnSmith());
    }
}
