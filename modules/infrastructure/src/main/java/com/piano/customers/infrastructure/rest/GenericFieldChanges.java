package com.piano.customers.infrastructure.rest;

import com.piano.customers.domain.FieldName;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

record GenericFieldChanges(Map<FieldName, Object> toSet, Set<FieldName> toRemove) {

    static GenericFieldChanges none() {
        return new GenericFieldChanges(Map.of(), Set.of());
    }

    static GenericFieldChanges fromJson(Iterable<Map.Entry<String, JsonNode>> fields) {
        Map<FieldName, Object> toSet = new HashMap<>();
        Set<FieldName> toRemove = new HashSet<>();
        for (Map.Entry<String, JsonNode> field : fields) {
            var name = new FieldName(field.getKey());
            JsonNode value = field.getValue();
            if (value.isNull()) {
                toRemove.add(name);
            } else {
                toSet.put(name, toRawValue(field.getKey(), value));
            }
        }
        return new GenericFieldChanges(toSet, toRemove);
    }

    /**
     * For create: there is nothing to remove yet, so a {@code null} value is rejected; omit the
     * field instead.
     */
    static Map<FieldName, Object> valuesFromJson(Iterable<Map.Entry<String, JsonNode>> fields) {
        Map<FieldName, Object> values = new HashMap<>();
        for (Map.Entry<String, JsonNode> field : fields) {
            var name = new FieldName(field.getKey());
            JsonNode value = field.getValue();
            if (value.isNull()) {
                throw new InvalidRequestException(
                        "Field "
                                + name.value()
                                + " must not be null when creating a customer; omit it instead");
            }
            values.put(name, toRawValue(field.getKey(), value));
        }
        return values;
    }

    // Deliberately the same mapping as GenericFieldsJson.toRawValue (persistence). The API and the
    // stored JSON share a
    // format today, but each may change on its own, and they fail differently: 400 here, 500 for
    // corrupt stored data.
    private static Object toRawValue(String name, JsonNode value) {
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isIntegralNumber()) {
            if (!value.canConvertToLong()) {
                throw new InvalidRequestException(
                        "Field "
                                + name
                                + " must be an integer between "
                                + Long.MIN_VALUE
                                + " and "
                                + Long.MAX_VALUE);
            }
            return value.longValue();
        }
        if (value.isString()) {
            return value.stringValue();
        }
        throw new InvalidRequestException(
                "Field " + name + " must be a boolean, an integer, a string or null");
    }
}
