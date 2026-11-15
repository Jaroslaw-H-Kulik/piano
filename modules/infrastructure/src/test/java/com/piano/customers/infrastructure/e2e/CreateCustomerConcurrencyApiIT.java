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
 * Concurrent {@code POST /customers}: the unique key in PostgreSQL, not a check-then-insert,
 * decides the winner.
 */
class CreateCustomerConcurrencyApiIT extends CustomerApiTest {

    private static final int ATTEMPTS = 8;

    @Test
    void exactlyOneOfManyConcurrentCreatesWins() throws Exception {
        // The latch releases all requests at once, so they really compete instead of running one
        // after another.
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);
        try {
            List<Future<ApiResponse>> futures = new ArrayList<>();
            for (int i = 0; i < ATTEMPTS; i++) {
                Callable<ApiResponse> create =
                        () -> {
                            go.await();
                            return tenantA.createCustomer(json("id", CUSTOMER_ID));
                        };
                futures.add(pool.submit(create));
            }
            go.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<ApiResponse> future : futures) {
                statuses.add(future.get().status());
            }
            assertThat(statuses.stream().filter(status -> status == HttpStatus.OK.value()).count())
                    .isEqualTo(1);
            assertThat(statuses.stream().filter(status -> status != HttpStatus.OK.value()))
                    .containsOnly(HttpStatus.CONFLICT.value());
            assertThat(jdbc.sql("SELECT count(*) FROM customer").query(Long.class).single())
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
