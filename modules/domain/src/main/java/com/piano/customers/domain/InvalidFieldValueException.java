package com.piano.customers.domain;

public class InvalidFieldValueException extends DomainValidationException {

    public InvalidFieldValueException(String message) {
        super(message);
    }
}
