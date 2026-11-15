package com.piano.customers.domain;

public class UnknownGenericFieldException extends DomainValidationException {

    public UnknownGenericFieldException(FieldName fieldName) {
        super("Generic field " + fieldName.value() + " is not registered for this tenant");
    }
}
