package com.piano.customers.domain;

public class DuplicateGenericFieldException extends RuntimeException {

    public DuplicateGenericFieldException(FieldName name) {
        super("Generic field " + name.value() + " is already registered for this tenant");
    }
}
