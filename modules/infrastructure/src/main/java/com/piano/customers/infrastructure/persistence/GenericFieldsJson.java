package com.piano.customers.infrastructure.persistence;

import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldValue;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts generic field values to and from the JSON text kept in the {@code generic_fields}
 * column. Stateless.
 */
final class GenericFieldsJson {

    // A JsonMapper is immutable and thread-safe, so one shared instance is fine.
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private GenericFieldsJson() {}

    static String toJson(Map<FieldName, FieldValue> fields) {
        Map<String, Object> rawValues = new HashMap<>();
        for (Map.Entry<FieldName, FieldValue> field : fields.entrySet()) {
            rawValues.put(field.getKey().value(), field.getValue().rawValue());
        }
        return MAPPER.writeValueAsString(rawValues);
    }

    static Map<FieldName, Object> fromJson(String jsonText) {
        JsonNode json = MAPPER.readTree(jsonText);
        Map<FieldName, Object> rawValues = new HashMap<>();
        for (Map.Entry<String, JsonNode> field : json.properties()) {
            rawValues.put(
                    new FieldName(field.getKey()), toRawValue(field.getKey(), field.getValue()));
        }
        return rawValues;
    }

    // Deliberately the same mapping as the REST GenericFieldChanges.toRawValue; see the comment
    // there.
    private static Object toRawValue(String name, JsonNode value) {
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            return value.longValue();
        }
        if (value.isString()) {
            return value.stringValue();
        }
        throw new IllegalStateException(
                "Stored generic field " + name + " has an unsupported JSON value");
    }
}
