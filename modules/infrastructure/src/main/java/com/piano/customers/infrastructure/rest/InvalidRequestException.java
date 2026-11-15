package com.piano.customers.infrastructure.rest;

class InvalidRequestException extends RuntimeException {

    InvalidRequestException(String message) {
        super(message);
    }
}
