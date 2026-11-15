package com.piano.customers.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The tenant, the key that keeps tenants' data apart. Restricted like {@link CustomerId}, so that
 * near-identical header values ({@code "tenant 1"}, {@code "tenant_1 "}) cannot silently name
 * different tenants.
 */
public record TenantId(String value) {

    public static final int MAX_LENGTH = 128;

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_LENGTH + "}");

    public TenantId {
        Objects.requireNonNull(value, "value");
        if (!ALLOWED.matcher(value).matches()) {
            throw new DomainValidationException(
                    "Tenant id must be 1-"
                            + MAX_LENGTH
                            + " characters: letters, digits, underscore or hyphen");
        }
    }
}
