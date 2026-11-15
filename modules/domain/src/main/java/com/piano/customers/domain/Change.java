package com.piano.customers.domain;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public sealed interface Change<T> {

    @Nullable T applyTo(@Nullable T current);

    static <T> Change<T> keep() {
        return new Keep<>();
    }

    static <T> Change<T> clear() {
        return new Clear<>();
    }

    static <T> Change<T> assign(T value) {
        return new Assign<>(value);
    }

    record Keep<T>() implements Change<T> {
        @Override
        public @Nullable T applyTo(@Nullable T current) {
            return current;
        }
    }

    record Clear<T>() implements Change<T> {
        @Override
        public @Nullable T applyTo(@Nullable T current) {
            return null;
        }
    }

    record Assign<T>(T value) implements Change<T> {
        public Assign {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public @Nullable T applyTo(@Nullable T current) {
            return value;
        }
    }
}
