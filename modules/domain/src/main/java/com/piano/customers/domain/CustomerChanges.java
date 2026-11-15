package com.piano.customers.domain;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Requested changes to a customer.
 *
 * <p>Names are tri-state ({@link Change}): keep, clear or assign. Generic fields need only two
 * collections, because a field that is not mentioned is simply kept.
 *
 * @param genericFieldsToSet raw values as received: {@code Boolean}, {@code Long} or {@code
 *     String}. They are typed by the field's definition ({@link GenericFieldDefinition#parse}),
 *     which is only known at runtime.
 * @param genericFieldsToRemove fields whose value is removed
 */
public record CustomerChanges(
        Change<String> firstName,
        Change<String> lastName,
        Map<FieldName, Object> genericFieldsToSet,
        Set<FieldName> genericFieldsToRemove) {

    public CustomerChanges {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(genericFieldsToSet, "genericFieldsToSet");
        Objects.requireNonNull(genericFieldsToRemove, "genericFieldsToRemove");
        genericFieldsToSet = Map.copyOf(genericFieldsToSet);
        genericFieldsToRemove = Set.copyOf(genericFieldsToRemove);
        for (FieldName name : genericFieldsToRemove) {
            if (genericFieldsToSet.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Generic field " + name.value() + " cannot be set and removed");
            }
        }
    }

    /** Every generic field this change touches, set or removed. */
    public Set<FieldName> genericFieldNames() {
        Set<FieldName> names = new HashSet<>(genericFieldsToSet.keySet());
        names.addAll(genericFieldsToRemove);
        return names;
    }
}
