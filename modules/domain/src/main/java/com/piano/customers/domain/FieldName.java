package com.piano.customers.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record FieldName(String value) {

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_]{1,64}");

    public FieldName {
        Objects.requireNonNull(value, "value");
        if (!ALLOWED.matcher(value).matches()) {
            throw new DomainValidationException(
                    "Field name must be 1-64 characters: letters, digits or underscore");
        }
    }
}
