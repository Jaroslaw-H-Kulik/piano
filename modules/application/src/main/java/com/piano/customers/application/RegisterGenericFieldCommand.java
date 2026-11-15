package com.piano.customers.application;

import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldType;
import com.piano.customers.domain.TenantId;

public record RegisterGenericFieldCommand(TenantId tenantId, FieldName name, FieldType type) {}
