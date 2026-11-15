package com.piano.customers.domain;

public sealed interface FieldValue permits BooleanValue, IntegerValue, StringValue, DateTimeValue {

    FieldType type();

    /**
     * The value in its raw form ({@code Boolean}, {@code Long} or {@code String}): the inverse of
     * {@link GenericFieldDefinition#parse}, so {@code definition.parse(value.rawValue())} returns
     * an equal value.
     */
    Object rawValue();
}
