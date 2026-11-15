package com.piano.customers.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.DomainValidationException;
import com.piano.customers.domain.FieldName;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GenericFieldChangesTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private GenericFieldChanges fromJson(String fields) {
        return GenericFieldChanges.fromJson(mapper.readTree(fields).properties());
    }

    @Test
    void setsEverySupportedJsonValueAndRemovesNulls() {
        GenericFieldChanges changes =
                fromJson(
                        "{\"vip\":true,\"score\":100,\"dob\":\"1990-01-01T00:00:00\",\"nick\":null}");

        assertThat(changes.toSet())
                .containsOnly(
                        Map.entry(new FieldName("vip"), true),
                        Map.entry(new FieldName("score"), 100L),
                        Map.entry(new FieldName("dob"), "1990-01-01T00:00:00"));
        assertThat(changes.toRemove()).containsOnly(new FieldName("nick"));
    }

    @Test
    void rejectsAnInvalidFieldName() {
        assertThatThrownBy(() -> fromJson("{\"has space\":1}"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNumbersThatAreNotIntegers() {
        assertThatThrownBy(() -> fromJson("{\"score\":1.5}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Field score must be a boolean, an integer, a string or null");
    }

    @Test
    void rejectsIntegersBeyondLongRange() {
        assertThatThrownBy(() -> fromJson("{\"score\":92233720368547758070}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(
                        "Field score must be an integer between -9223372036854775808 and 9223372036854775807");
    }

    @Test
    void rejectsArraysAndObjectsAsFieldValues() {
        assertThatThrownBy(() -> fromJson("{\"tags\":[1]}"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> fromJson("{\"tags\":{}}"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
