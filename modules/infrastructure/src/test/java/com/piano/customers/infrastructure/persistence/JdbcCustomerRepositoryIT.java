package com.piano.customers.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.BooleanValue;
import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.CustomerModifiedConcurrentlyException;
import com.piano.customers.domain.DateTimeValue;
import com.piano.customers.domain.DuplicateCustomerException;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldType;
import com.piano.customers.domain.FieldValue;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.IntegerValue;
import com.piano.customers.domain.StringValue;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({JdbcCustomerRepository.class, JdbcGenericFieldDefinitionRepository.class})
class JdbcCustomerRepositoryIT extends PostgresRepositoryTest {

    private static final TenantId TENANT = new TenantId("tenant_123");
    private static final TenantId OTHER_TENANT = new TenantId("tenant_456");
    private static final CustomerId CUSTOMER_ID = new CustomerId("cus_123");

    private static final FieldName DOB = new FieldName("dob");
    private static final FieldName VIP = new FieldName("vip");
    private static final FieldName SCORE = new FieldName("score");
    private static final FieldName NICKNAME = new FieldName("nickname");

    @Autowired private JdbcCustomerRepository customers;

    @Autowired private JdbcGenericFieldDefinitionRepository definitions;

    @BeforeEach
    void registerFieldsForBothTenants() {
        for (TenantId tenant : new TenantId[] {TENANT, OTHER_TENANT}) {
            definitions.insert(new GenericFieldDefinition(tenant, DOB, FieldType.DATETIME));
            definitions.insert(new GenericFieldDefinition(tenant, VIP, FieldType.BOOL));
            definitions.insert(new GenericFieldDefinition(tenant, SCORE, FieldType.INTEGER));
            definitions.insert(new GenericFieldDefinition(tenant, NICKNAME, FieldType.STRING));
        }
    }

    private static Customer customerWith(
            TenantId tenant, CustomerId id, Map<FieldName, FieldValue> fields) {
        return new Customer(tenant, id, "John", "Smith", fields, 0);
    }

    private static Map<FieldName, FieldValue> allFourTypes() {
        return Map.of(
                DOB, DateTimeValue.parse("1990-01-01T00:00:00"),
                VIP, new BooleanValue(true),
                SCORE, new IntegerValue(100),
                NICKNAME, new StringValue("1990-01-01T00:00:00"));
    }

    @Test
    void storesAndReadsBackACustomerWithAllFieldTypes() {
        Customer customer = customerWith(TENANT, CUSTOMER_ID, allFourTypes());

        customers.insert(customer);

        assertThat(customers.findById(TENANT, CUSTOMER_ID)).contains(customer);
    }

    @Test
    void keepsAStringThatLooksLikeADatetimeAsAString() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, allFourTypes()));

        Customer read = customers.findById(TENANT, CUSTOMER_ID).orElseThrow();

        assertThat(read.genericFields().get(NICKNAME))
                .isEqualTo(new StringValue("1990-01-01T00:00:00"));
        assertThat(read.genericFields().get(DOB)).isInstanceOf(DateTimeValue.class);
    }

    @Test
    void storesACustomerWithOnlyAnId() {
        Customer onlyId = new Customer(TENANT, CUSTOMER_ID, null, null, Map.of(), 0);

        customers.insert(onlyId);

        assertThat(customers.findById(TENANT, CUSTOMER_ID)).contains(onlyId);
    }

    @Test
    void roundTripsTheExtremesOfInteger() {
        Map<FieldName, FieldValue> fields = Map.of(SCORE, new IntegerValue(Long.MIN_VALUE));
        customers.insert(customerWith(TENANT, CUSTOMER_ID, fields));

        assertThat(customers.findById(TENANT, CUSTOMER_ID).orElseThrow().genericFields())
                .isEqualTo(fields);
    }

    @Test
    void returnsNothingForAnUnknownCustomer() {
        assertThat(customers.findById(TENANT, CUSTOMER_ID)).isEmpty();
    }

    @Test
    void rejectsTheSameIdTwiceInOneTenant() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, Map.of()));

        assertThatThrownBy(() -> customers.insert(customerWith(TENANT, CUSTOMER_ID, Map.of())))
                .isInstanceOf(DuplicateCustomerException.class);
    }

    @Test
    void allowsTheSameIdInTwoTenantsAndKeepsThemApart() {
        customers.insert(new Customer(TENANT, CUSTOMER_ID, "John", null, Map.of(), 0));
        customers.insert(new Customer(OTHER_TENANT, CUSTOMER_ID, "Jane", null, Map.of(), 0));

        assertThat(customers.findById(TENANT, CUSTOMER_ID).orElseThrow().firstName())
                .isEqualTo("John");
        assertThat(customers.findById(OTHER_TENANT, CUSTOMER_ID).orElseThrow().firstName())
                .isEqualTo("Jane");
    }

    @Test
    void updatesAndIncreasesTheVersion() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, allFourTypes()));
        Customer changed =
                new Customer(
                        TENANT,
                        CUSTOMER_ID,
                        "Lewis",
                        null,
                        Map.of(VIP, new BooleanValue(false)),
                        0);

        Customer saved = customers.update(changed);

        assertThat(saved.version()).isEqualTo(1);
        assertThat(customers.findById(TENANT, CUSTOMER_ID)).contains(saved);
        assertThat(saved.lastName()).isNull();
        assertThat(saved.genericFields()).containsOnlyKeys(VIP);
    }

    @Test
    void rejectsAnUpdateBasedOnAStaleVersion() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, Map.of()));
        Customer firstReader = customers.findById(TENANT, CUSTOMER_ID).orElseThrow();
        Customer secondReader = customers.findById(TENANT, CUSTOMER_ID).orElseThrow();
        customers.update(
                new Customer(
                        TENANT, CUSTOMER_ID, "First", "Smith", Map.of(), firstReader.version()));

        Customer staleChange =
                new Customer(
                        TENANT, CUSTOMER_ID, "Second", "Smith", Map.of(), secondReader.version());

        assertThatThrownBy(() -> customers.update(staleChange))
                .isInstanceOf(CustomerModifiedConcurrentlyException.class);
        assertThat(customers.findById(TENANT, CUSTOMER_ID).orElseThrow().firstName())
                .isEqualTo("First");
    }

    @Test
    void reportsAConflictWhenUpdatingAMissingCustomer() {
        assertThatThrownBy(() -> customers.update(customerWith(TENANT, CUSTOMER_ID, Map.of())))
                .isInstanceOf(CustomerModifiedConcurrentlyException.class);
    }

    @Test
    void doesNotUpdateACustomerOfAnotherTenant() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, Map.of()));

        assertThatThrownBy(
                        () -> customers.update(customerWith(OTHER_TENANT, CUSTOMER_ID, Map.of())))
                .isInstanceOf(CustomerModifiedConcurrentlyException.class);
        assertThat(customers.findById(TENANT, CUSTOMER_ID).orElseThrow().version()).isZero();
    }

    @Test
    void deletesACustomer() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, allFourTypes()));

        boolean deleted = customers.delete(TENANT, CUSTOMER_ID);

        assertThat(deleted).isTrue();
        assertThat(customers.findById(TENANT, CUSTOMER_ID)).isEmpty();
    }

    @Test
    void reportsThatNothingWasDeleted() {
        assertThat(customers.delete(TENANT, CUSTOMER_ID)).isFalse();
    }

    @Test
    void doesNotDeleteACustomerOfAnotherTenant() {
        customers.insert(customerWith(TENANT, CUSTOMER_ID, Map.of()));

        assertThat(customers.delete(OTHER_TENANT, CUSTOMER_ID)).isFalse();
        assertThat(customers.findById(TENANT, CUSTOMER_ID)).isPresent();
    }
}
