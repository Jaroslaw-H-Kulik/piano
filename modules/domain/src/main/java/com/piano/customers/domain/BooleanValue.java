package com.piano.customers.domain;

public record BooleanValue(boolean value) implements FieldValue {

    @Override
    public FieldType type() {
        return FieldType.BOOL;
    }

    @Override
    public Boolean rawValue() {
        return value;
    }
}
