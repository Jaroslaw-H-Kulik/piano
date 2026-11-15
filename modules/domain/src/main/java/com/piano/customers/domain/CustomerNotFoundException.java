package com.piano.customers.domain;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(CustomerId id) {
        super("Customer " + id.value() + " was not found");
    }
}
