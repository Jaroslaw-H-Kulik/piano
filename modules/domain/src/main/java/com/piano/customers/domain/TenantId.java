package com.piano.customers.domain;

public record TenantId(String value) {

    public static final int MAX_LENGTH = 128;

    public TenantId {
        Validation.requireText(value, "Tenant id", MAX_LENGTH);
    }
}
