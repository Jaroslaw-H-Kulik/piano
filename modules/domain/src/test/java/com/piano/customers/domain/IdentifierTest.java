package com.piano.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifierTest {

    @Test
    void tenantIdKeepsItsValue() {
        assertThat(new TenantId("tenant_123").value()).isEqualTo("tenant_123");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t"})
    void tenantIdRejectsBlankValues(String blank) {
        assertThatThrownBy(() -> new TenantId(blank))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Tenant id must not be blank");
    }

    @Test
    void tenantIdRejectsTooLongValues() {
        String tooLong = "t".repeat(TenantId.MAX_LENGTH + 1);

        assertThatThrownBy(() -> new TenantId(tooLong))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Tenant id must not be longer than 128 characters");
    }

    @Test
    void tenantIdAcceptsMaximumLength() {
        String longest = "t".repeat(TenantId.MAX_LENGTH);

        assertThat(new TenantId(longest).value()).isEqualTo(longest);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cus_123", "A", "Mixed_Case-9", "123e4567-e89b-12d3-a456-426614174000"})
    void customerIdAcceptsLettersDigitsUnderscoreAndHyphen(String id) {
        assertThat(new CustomerId(id).value()).isEqualTo(id);
    }

    @Test
    void customerIdAcceptsMaximumLength() {
        String longest = "c".repeat(CustomerId.MAX_LENGTH);

        assertThat(new CustomerId(longest).value()).isEqualTo(longest);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(
            strings = {
                " ", "\t", " cus_1", "cus 1", "a/b", "a;b", ".", "..", "a.b", "a%2Fb", "a?b", "a#b",
                "zażółć"
            })
    void customerIdRejectsCharactersThatAreNotUrlSafe(String invalid) {
        assertThatThrownBy(() -> new CustomerId(invalid))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage(
                        "Customer id must be 1-128 characters: letters, digits, underscore or hyphen");
    }

    @Test
    void customerIdRejectsTooLongValues() {
        String tooLong = "c".repeat(CustomerId.MAX_LENGTH + 1);

        assertThatThrownBy(() -> new CustomerId(tooLong))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage(
                        "Customer id must be 1-128 characters: letters, digits, underscore or hyphen");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dob", "score_2", "A", "Mixed_Case_9"})
    void fieldNameAcceptsLettersDigitsAndUnderscore(String name) {
        assertThat(new FieldName(name).value()).isEqualTo(name);
    }

    @Test
    void fieldNameAcceptsMaximumLength() {
        String longest = "f".repeat(64);

        assertThat(new FieldName(longest).value()).isEqualTo(longest);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "has space", "dash-ed", "dot.ted", "zażółć", "a/b"})
    void fieldNameRejectsOtherCharacters(String invalid) {
        assertThatThrownBy(() -> new FieldName(invalid))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Field name must be 1-64 characters: letters, digits or underscore");
    }

    @Test
    void fieldNameRejectsTooLongValues() {
        String tooLong = "f".repeat(65);

        assertThatThrownBy(() -> new FieldName(tooLong))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void fieldNamesAreCaseSensitive() {
        assertThat(new FieldName("dob")).isNotEqualTo(new FieldName("DOB"));
    }

    @Test
    void nullIdentifiersAreProgrammingErrors() {
        assertThatThrownBy(() -> new TenantId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CustomerId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FieldName(null)).isInstanceOf(NullPointerException.class);
    }
}
