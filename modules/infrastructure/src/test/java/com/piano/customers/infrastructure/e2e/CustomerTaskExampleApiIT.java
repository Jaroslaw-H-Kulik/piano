package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * The example from the task, replayed step by step as a smoke test of the whole customer lifecycle.
 */
class CustomerTaskExampleApiIT extends CustomerApiTest {

    @Test
    void replaysTheExampleFromTheTask() {
        registerTaskExampleFields(tenantA);

        assertCustomer(tenantA.createCustomer(johnSmith()), johnSmith());
        assertStored(TENANT_A, johnSmith());
        assertCustomer(tenantA.getCustomer(CUSTOMER_ID), johnSmith());

        ApiResponse patched =
                tenantA.patchCustomer(
                        CUSTOMER_ID,
                        json(
                                "first_name",
                                "Lewis",
                                "last_name",
                                "Bull",
                                "generic_fields",
                                json("dob", null, "vip", false)));
        Map<String, Object> expectedAfterPatch =
                customer(CUSTOMER_ID, "Lewis", "Bull", json("vip", false, "score", 100));
        assertCustomer(patched, expectedAfterPatch);
        assertStored(TENANT_A, expectedAfterPatch);
        assertCustomer(tenantA.getCustomer(CUSTOMER_ID), expectedAfterPatch);

        assertStatus(tenantA.deleteCustomer(CUSTOMER_ID), HttpStatus.NO_CONTENT);
        assertNotStored(TENANT_A, CUSTOMER_ID);
        assertCustomerNotFound(tenantA.getCustomer(CUSTOMER_ID), CUSTOMER_ID);
    }
}
