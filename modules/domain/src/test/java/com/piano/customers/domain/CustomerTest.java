package com.piano.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final TenantId TENANT = new TenantId("tenant_123");
    private static final CustomerId CUSTOMER_ID = new CustomerId("cus_123");

    private static final FieldName DOB = new FieldName("dob");
    private static final FieldName VIP = new FieldName("vip");
    private static final FieldName SCORE = new FieldName("score");
    private static final FieldName NICKNAME = new FieldName("nickname");

    private final Map<FieldName, GenericFieldDefinition> definitions =
            Map.of(
                    DOB, new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME),
                    VIP, new GenericFieldDefinition(TENANT, VIP, FieldType.BOOL),
                    SCORE, new GenericFieldDefinition(TENANT, SCORE, FieldType.INTEGER),
                    NICKNAME, new GenericFieldDefinition(TENANT, NICKNAME, FieldType.STRING));

    private static CustomerChanges names(Change<String> firstName, Change<String> lastName) {
        return new CustomerChanges(firstName, lastName, Map.of(), Set.of());
    }

    private static CustomerChanges setting(Map<FieldName, Object> genericFieldsToSet) {
        return new CustomerChanges(Change.keep(), Change.keep(), genericFieldsToSet, Set.of());
    }

    private static CustomerChanges removing(FieldName... genericFieldsToRemove) {
        return new CustomerChanges(
                Change.keep(), Change.keep(), Map.of(), Set.of(genericFieldsToRemove));
    }

    private Customer johnSmith() {
        Map<FieldName, Object> genericFields =
                Map.of(DOB, "1990-01-01T00:00:00", VIP, true, SCORE, 100L);

        return Customer.create(TENANT, CUSTOMER_ID, "John", "Smith", genericFields, definitions);
    }

    @Test
    void createsCustomerWithOnlyAPublicId() {
        Customer customer = Customer.create(TENANT, CUSTOMER_ID, null, null, Map.of(), definitions);

        assertThat(customer.tenantId()).isEqualTo(TENANT);
        assertThat(customer.id()).isEqualTo(CUSTOMER_ID);
        assertThat(customer.firstName()).isNull();
        assertThat(customer.lastName()).isNull();
        assertThat(customer.genericFields()).isEmpty();
        assertThat(customer.version()).isZero();
    }

    @Test
    void createsCustomerFromTheTaskExample() {
        Customer customer = johnSmith();

        assertThat(customer.firstName()).isEqualTo("John");
        assertThat(customer.lastName()).isEqualTo("Smith");
        assertThat(customer.genericFields())
                .containsOnly(
                        Map.entry(DOB, new DateTimeValue(LocalDateTime.of(1990, 1, 1, 0, 0, 0))),
                        Map.entry(VIP, new BooleanValue(true)),
                        Map.entry(SCORE, new IntegerValue(100)));
    }

    @Test
    void rejectsCreationWithAnUnknownGenericField() {
        Map<FieldName, Object> genericFields = Map.of(new FieldName("shoe_size"), 42L);

        assertThatThrownBy(
                        () ->
                                Customer.create(
                                        TENANT,
                                        CUSTOMER_ID,
                                        null,
                                        null,
                                        genericFields,
                                        definitions))
                .isInstanceOf(UnknownGenericFieldException.class)
                .hasMessage("Generic field shoe_size is not registered for this tenant");
    }

    @Test
    void rejectsCreationWithABlankFirstName() {
        assertThatThrownBy(
                        () ->
                                Customer.create(
                                        TENANT, CUSTOMER_ID, " ", null, Map.of(), definitions))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("First name must not be blank");
    }

    @Test
    void rejectsANameContainingTheNulCharacter() {
        assertThatThrownBy(
                        () ->
                                Customer.create(
                                        TENANT,
                                        CUSTOMER_ID,
                                        "Jo\u0000hn",
                                        null,
                                        Map.of(),
                                        definitions))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("First name must not contain the NUL character (U+0000)");
    }

    @Test
    void rejectsAStringFieldValueContainingTheNulCharacter() {
        Map<FieldName, Object> genericFields = Map.of(NICKNAME, "Jo\u0000hn");

        assertThatThrownBy(
                        () ->
                                Customer.create(
                                        TENANT,
                                        CUSTOMER_ID,
                                        null,
                                        null,
                                        genericFields,
                                        definitions))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("String value must not contain the NUL character (U+0000)");
    }

    @Test
    void updateFromTheTaskExampleRemovesDobKeepsScoreAndReplacesTheRest() {
        var update =
                new CustomerChanges(
                        Change.assign("Lewis"),
                        Change.assign("Bull"),
                        Map.of(VIP, false),
                        Set.of(DOB));

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.firstName()).isEqualTo("Lewis");
        assertThat(updated.lastName()).isEqualTo("Bull");
        assertThat(updated.genericFields())
                .containsOnly(
                        Map.entry(VIP, new BooleanValue(false)),
                        Map.entry(SCORE, new IntegerValue(100)));
    }

    @Test
    void keepsEverythingWhenNothingIsChanged() {
        Customer original = johnSmith();
        CustomerChanges nothing = names(Change.keep(), Change.keep());

        Customer updated = original.applyChanges(nothing, definitions);

        assertThat(updated).isEqualTo(original);
    }

    @Test
    void omittedGenericFieldsAreKept() {
        CustomerChanges update = setting(Map.of(VIP, false));

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.genericFields()).containsKeys(DOB, SCORE);
    }

    @Test
    void assigningAGenericFieldReplacesItsValue() {
        CustomerChanges update = setting(Map.of(SCORE, 250L));

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.genericFields()).containsEntry(SCORE, new IntegerValue(250));
    }

    @Test
    void assigningAGenericFieldThatWasNotSetAddsIt() {
        CustomerChanges update = setting(Map.of(NICKNAME, "Johnny"));

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.genericFields()).containsEntry(NICKNAME, new StringValue("Johnny"));
    }

    @Test
    void clearingAGenericFieldThatWasNeverSetChangesNothing() {
        Customer original = johnSmith();
        CustomerChanges update = removing(NICKNAME);

        Customer updated = original.applyChanges(update, definitions);

        assertThat(updated).isEqualTo(original);
    }

    @Test
    void changesFirstNameWithoutTouchingLastName() {
        CustomerChanges update = names(Change.assign("Lewis"), Change.keep());

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.firstName()).isEqualTo("Lewis");
        assertThat(updated.lastName()).isEqualTo("Smith");
    }

    @Test
    void clearsNames() {
        CustomerChanges update = names(Change.clear(), Change.clear());

        Customer updated = johnSmith().applyChanges(update, definitions);

        assertThat(updated.firstName()).isNull();
        assertThat(updated.lastName()).isNull();
    }

    @Test
    void doesNotModifyTheOriginalCustomer() {
        Customer original = johnSmith();
        CustomerChanges update =
                new CustomerChanges(Change.assign("Lewis"), Change.keep(), Map.of(), Set.of(SCORE));

        original.applyChanges(update, definitions);

        assertThat(original.firstName()).isEqualTo("John");
        assertThat(original.genericFields()).containsKey(SCORE);
    }

    @Test
    void keepsTheVersionBecausePersistenceIncreasesIt() {
        Customer stored = new Customer(TENANT, CUSTOMER_ID, "John", "Smith", Map.of(), 7);
        CustomerChanges update = names(Change.assign("Lewis"), Change.keep());

        Customer updated = stored.applyChanges(update, definitions);

        assertThat(updated.version()).isEqualTo(7);
    }

    @Test
    void rejectsAnUnknownGenericField() {
        FieldName unknown = new FieldName("shoe_size");
        CustomerChanges update = setting(Map.of(unknown, 42L));

        assertThatThrownBy(() -> johnSmith().applyChanges(update, definitions))
                .isInstanceOf(UnknownGenericFieldException.class)
                .hasMessage("Generic field shoe_size is not registered for this tenant");
    }

    @Test
    void rejectsClearingAnUnknownGenericField() {
        FieldName unknown = new FieldName("shoe_size");
        CustomerChanges update = removing(unknown);

        assertThatThrownBy(() -> johnSmith().applyChanges(update, definitions))
                .isInstanceOf(UnknownGenericFieldException.class);
    }

    @Test
    void rejectsAValueOfTheWrongType() {
        CustomerChanges update = setting(Map.of(SCORE, "high"));

        assertThatThrownBy(() -> johnSmith().applyChanges(update, definitions))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field score expects INTEGER but received a string");
    }

    @Test
    void rejectsCreationWithAValueOfTheWrongType() {
        Map<FieldName, Object> genericFields = Map.of(VIP, "yes");

        assertThatThrownBy(
                        () ->
                                Customer.create(
                                        TENANT,
                                        CUSTOMER_ID,
                                        null,
                                        null,
                                        genericFields,
                                        definitions))
                .isInstanceOf(InvalidFieldValueException.class);
    }

    @Test
    void rejectsABlankFirstName() {
        CustomerChanges update = names(Change.assign("  "), Change.keep());

        assertThatThrownBy(() -> johnSmith().applyChanges(update, definitions))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("First name must not be blank");
    }

    @Test
    void rejectsATooLongLastName() {
        String tooLong = "x".repeat(Customer.MAX_NAME_LENGTH + 1);
        CustomerChanges update = names(Change.keep(), Change.assign(tooLong));

        assertThatThrownBy(() -> johnSmith().applyChanges(update, definitions))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Last name must not be longer than 255 characters");
    }

    @Test
    void copiesTheGenericFieldsMapSoOutsideChangesAreNotVisible() {
        Map<FieldName, FieldValue> fields = new HashMap<>();
        fields.put(SCORE, new IntegerValue(1));
        Customer customer = new Customer(TENANT, CUSTOMER_ID, null, null, fields, 0);

        fields.put(VIP, new BooleanValue(true));

        assertThat(customer.genericFields()).containsOnlyKeys(SCORE);
    }
}
