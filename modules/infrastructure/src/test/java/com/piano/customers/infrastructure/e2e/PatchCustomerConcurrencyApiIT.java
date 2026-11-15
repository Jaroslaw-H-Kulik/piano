package com.piano.customers.infrastructure.e2e;

import static com.piano.customers.infrastructure.e2e.helper.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.piano.customers.infrastructure.e2e.helper.ApiResponse;
import com.piano.customers.infrastructure.e2e.helper.CustomerApiTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Concurrent {@code PATCH /customers/{id}}: the version check in PostgreSQL detects competing
 * writes and the service re-applies the patch to the newer state (at most 3 attempts in total), so
 * patches of different attributes all win.
 */
class PatchCustomerConcurrencyApiIT extends CustomerApiTest {

    @Test
    void concurrentPatchesOfDifferentAttributesAreAllApplied() throws Exception {
        // Three competitors can make an attempt fail at most twice (each failure means another one
        // succeeded),
        // so the third attempt of every request is guaranteed to succeed.
        givenJohnSmith(tenantA);
        List<Callable<ApiResponse>> patches =
                List.of(
                        () -> tenantA.patchCustomer(CUSTOMER_ID, json("first_name", "Lewis")),
                        () -> tenantA.patchCustomer(CUSTOMER_ID, json("last_name", "Bull")),
                        () ->
                                tenantA.patchCustomer(
                                        CUSTOMER_ID, json("generic_fields", json("score", 7))));

        List<ApiResponse> responses = runAtTheSameTime(patches);

        responses.forEach(response -> assertStatus(response, HttpStatus.OK));
        assertStored(
                TENANT_A,
                customer(CUSTOMER_ID, "Lewis", "Bull", json("dob", DOB, "vip", true, "score", 7)));
    }

    @Test
    void aPatchRacingWithADeleteEndsInOneOfTheTwoConsistentOutcomes() throws Exception {
        givenJohnSmith(tenantA);
        List<Callable<ApiResponse>> requests =
                List.of(
                        () -> tenantA.patchCustomer(CUSTOMER_ID, json("first_name", "Lewis")),
                        () -> tenantA.deleteCustomer(CUSTOMER_ID));

        List<ApiResponse> responses = runAtTheSameTime(requests);

        ApiResponse patch = responses.get(0);
        assertStatus(responses.get(1), HttpStatus.NO_CONTENT);
        assertThat(patch.status()).isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
        assertNotStored(TENANT_A, CUSTOMER_ID);
    }

    /**
     * Releases all requests at once, so they really compete instead of running one after another.
     */
    private static List<ApiResponse> runAtTheSameTime(List<Callable<ApiResponse>> requests)
            throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<ApiResponse>> futures = new ArrayList<>();
            for (Callable<ApiResponse> request : requests) {
                futures.add(
                        pool.submit(
                                () -> {
                                    go.await();
                                    return request.call();
                                }));
            }
            go.countDown();

            List<ApiResponse> responses = new ArrayList<>();
            for (Future<ApiResponse> future : futures) {
                responses.add(future.get());
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }
}
