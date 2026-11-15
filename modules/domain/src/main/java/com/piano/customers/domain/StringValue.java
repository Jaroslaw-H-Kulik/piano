package com.piano.customers.domain;

import java.util.Objects;

public record StringValue(String value) implements FieldValue {

    public StringValue {
        Objects.requireNonNull(value, "value");
        Validation.requireNoNulCharacter(value, "String value");
    }

    @Override
    public FieldType type() {
        return FieldType.STRING;
    }

    @Override
    public String rawValue() {
        return value;
    }
}
