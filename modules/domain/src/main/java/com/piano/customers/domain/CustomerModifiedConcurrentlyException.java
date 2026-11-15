package com.piano.customers.domain;

public class CustomerModifiedConcurrentlyException extends RuntimeException {

    public CustomerModifiedConcurrentlyException(CustomerId id) {
        super("Customer " + id.value() + " was modified concurrently; read it again and retry");
    }
}
