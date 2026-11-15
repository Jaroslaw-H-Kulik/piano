package com.piano.customers.domain;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

final class Validation {

    private Validation() {}

    static void requireTextOrNull(@Nullable String value, String description, int maxLength) {
        if (value != null) {
            requireText(value, description, maxLength);
        }
    }

    static void requireText(String value, String description, int maxLength) {
        Objects.requireNonNull(value, description);
        requireNoNulCharacter(value, description);
        if (value.isBlank()) {
            throw new DomainValidationException(description + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new DomainValidationException(
                    description + " must not be longer than " + maxLength + " characters");
        }
    }

    /**
     * PostgreSQL cannot store U+0000 in text or jsonb; rejecting it here keeps it a 400 instead of
     * a database error.
     */
    static void requireNoNulCharacter(String value, String description) {
        if (value.indexOf('\u0000') >= 0) {
            throw new DomainValidationException(
                    description + " must not contain the NUL character (U+0000)");
        }
    }
}
