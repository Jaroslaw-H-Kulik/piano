package com.piano.customers.infrastructure.rest;

import com.piano.customers.domain.GenericFieldDefinition;

public record GenericFieldResponse(String name, String type) {

    public static GenericFieldResponse from(GenericFieldDefinition definition) {
        return new GenericFieldResponse(definition.name().value(), definition.type().name());
    }
}
