package com.piano.customers.infrastructure.rest;

import com.piano.customers.application.CreateCustomerCommand;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record CreateCustomerRequest(
        @Nullable String id,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable Map<String, JsonNode> genericFields) {

    CreateCustomerCommand toCommand(TenantId tenantId) {
        if (id == null) {
            throw new InvalidRequestException("Property id is required and must be a string");
        }
        return new CreateCustomerCommand(
                tenantId, new CustomerId(id), firstName, lastName, toGenericFields());
    }

    private Map<FieldName, Object> toGenericFields() {
        if (genericFields == null) {
            return Map.of();
        }
        return GenericFieldChanges.valuesFromJson(genericFields.entrySet());
    }
}
