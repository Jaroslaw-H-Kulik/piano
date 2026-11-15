package com.piano.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerChangesTest {

    private static final FieldName VIP = new FieldName("vip");
    private static final FieldName DOB = new FieldName("dob");

    @Test
    void genericFieldNamesContainSetAndRemovedFields() {
        var changes =
                new CustomerChanges(Change.keep(), Change.keep(), Map.of(VIP, false), Set.of(DOB));

        assertThat(changes.genericFieldNames()).containsExactlyInAnyOrder(VIP, DOB);
    }

    @Test
    void aFieldCannotBeSetAndRemovedAtTheSameTime() {
        assertThatThrownBy(
                        () ->
                                new CustomerChanges(
                                        Change.keep(),
                                        Change.keep(),
                                        Map.of(VIP, true),
                                        Set.of(VIP)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Generic field vip cannot be set and removed");
    }
}
