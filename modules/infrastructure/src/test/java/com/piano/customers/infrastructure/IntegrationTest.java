package com.piano.customers.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    @DynamicPropertySource
    static void useTestDatabase(DynamicPropertyRegistry registry) {
        TestPostgres.registerDataSource(registry);
    }

    @Autowired protected JdbcClient jdbc;

    @BeforeEach
    void emptyTables() {
        jdbc.sql("DELETE FROM customer").update();
        jdbc.sql("DELETE FROM generic_field_definition").update();
    }
}
