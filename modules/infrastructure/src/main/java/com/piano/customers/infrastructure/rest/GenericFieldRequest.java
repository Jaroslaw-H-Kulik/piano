package com.piano.customers.infrastructure.rest;

import com.piano.customers.application.RegisterGenericFieldCommand;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldType;
import com.piano.customers.domain.TenantId;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

public record GenericFieldRequest(@Nullable String name, @Nullable String type) {

    RegisterGenericFieldCommand toCommand(TenantId tenantId) {
        if (name == null) {
            throw new InvalidRequestException("Property name is required and must be a string");
        }
        if (type == null) {
            throw new InvalidRequestException("Property type is required and must be a string");
        }
        return new RegisterGenericFieldCommand(tenantId, new FieldName(name), toFieldType(type));
    }

    private static FieldType toFieldType(String text) {
        try {
            return FieldType.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(
                    "Property type must be one of " + Arrays.toString(FieldType.values()));
        }
    }
}
