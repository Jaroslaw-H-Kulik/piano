package com.piano.customers.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The one PostgreSQL container of the test run, shared by the API and the repository tests. It is
 * started once and never stopped by a test class (Testcontainers removes it when the JVM exits), so
 * a Spring context cached across test classes never points at a stopped container.
 */
public final class TestPostgres {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    private TestPostgres() {}

    public static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
