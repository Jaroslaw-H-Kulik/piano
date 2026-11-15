package com.piano.customers.domain;

import java.util.Map;
import java.util.Set;

public interface GenericFieldDefinitionRepository {

    void insert(GenericFieldDefinition definition);

    Map<FieldName, GenericFieldDefinition> findByNames(TenantId tenantId, Set<FieldName> names);
}
