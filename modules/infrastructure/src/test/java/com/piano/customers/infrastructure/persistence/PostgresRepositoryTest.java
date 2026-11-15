package com.piano.customers.infrastructure.persistence;

import com.piano.customers.infrastructure.TestPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PostgresRepositoryTest {

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
