package com.piano.customers.domain;

public record IntegerValue(long value) implements FieldValue {

    @Override
    public FieldType type() {
        return FieldType.INTEGER;
    }

    @Override
    public Long rawValue() {
        return value;
    }
}
