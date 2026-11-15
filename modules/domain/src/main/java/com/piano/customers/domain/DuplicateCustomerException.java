package com.piano.customers.domain;

public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(CustomerId id) {
        super("Customer " + id.value() + " already exists");
    }
}
