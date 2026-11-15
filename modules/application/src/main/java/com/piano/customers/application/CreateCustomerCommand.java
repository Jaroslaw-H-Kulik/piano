package com.piano.customers.application;

import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record CreateCustomerCommand(
        TenantId tenantId,
        CustomerId id,
        @Nullable String firstName,
        @Nullable String lastName,
        Map<FieldName, Object> genericFields) {

    public CreateCustomerCommand {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(genericFields, "genericFields");
        genericFields = Map.copyOf(genericFields);
    }
}
