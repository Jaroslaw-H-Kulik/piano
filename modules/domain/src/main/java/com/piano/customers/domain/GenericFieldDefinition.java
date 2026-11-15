package com.piano.customers.domain;

import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record GenericFieldDefinition(TenantId tenantId, FieldName name, FieldType type) {

    public GenericFieldDefinition {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    public FieldValue parse(@Nullable Object rawValue) {
        return switch (type) {
            case BOOL -> parseBoolean(rawValue);
            case INTEGER -> parseInteger(rawValue);
            case STRING -> parseString(rawValue);
            case DATETIME -> parseDateTime(rawValue);
        };
    }

    private FieldValue parseBoolean(@Nullable Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return new BooleanValue(booleanValue);
        }
        throw wrongType(rawValue);
    }

    private FieldValue parseInteger(@Nullable Object rawValue) {
        if (rawValue instanceof Long longValue) {
            return new IntegerValue(longValue);
        }
        throw wrongType(rawValue);
    }

    private FieldValue parseString(@Nullable Object rawValue) {
        if (rawValue instanceof String stringValue) {
            return new StringValue(stringValue);
        }
        throw wrongType(rawValue);
    }

    private FieldValue parseDateTime(@Nullable Object rawValue) {
        if (!(rawValue instanceof String text)) {
            throw wrongType(rawValue);
        }
        try {
            return DateTimeValue.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidFieldValueException(
                    "Field "
                            + name.value()
                            + " expects a DATETIME in "
                            + DateTimeValue.FORMAT_DESCRIPTION);
        }
    }

    private InvalidFieldValueException wrongType(@Nullable Object rawValue) {
        return new InvalidFieldValueException(
                "Field "
                        + name.value()
                        + " expects "
                        + type
                        + " but received "
                        + describe(rawValue));
    }

    private static String describe(@Nullable Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        if (rawValue instanceof Boolean) {
            return "a boolean";
        }
        if (rawValue instanceof Long) {
            return "an integer";
        }
        if (rawValue instanceof String) {
            return "a string";
        }
        return "an unsupported value";
    }
}
