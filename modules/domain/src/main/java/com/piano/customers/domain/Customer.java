package com.piano.customers.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record Customer(
        TenantId tenantId,
        CustomerId id,
        @Nullable String firstName,
        @Nullable String lastName,
        Map<FieldName, FieldValue> genericFields,
        long version) {

    public static final int MAX_NAME_LENGTH = 255;

    public Customer {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(id, "id");
        Validation.requireTextOrNull(firstName, "First name", MAX_NAME_LENGTH);
        Validation.requireTextOrNull(lastName, "Last name", MAX_NAME_LENGTH);
        Objects.requireNonNull(genericFields, "genericFields");
        genericFields = Map.copyOf(genericFields);
    }

    public static Customer create(
            TenantId tenantId,
            CustomerId id,
            @Nullable String firstName,
            @Nullable String lastName,
            Map<FieldName, Object> rawGenericFields,
            Map<FieldName, GenericFieldDefinition> definitions) {
        Map<FieldName, FieldValue> genericFields = new HashMap<>();

        for (Map.Entry<FieldName, Object> rawField : rawGenericFields.entrySet()) {
            FieldName fieldName = rawField.getKey();
            GenericFieldDefinition definition = requireDefinition(fieldName, definitions);
            genericFields.put(fieldName, definition.parse(rawField.getValue()));
        }

        return new Customer(tenantId, id, firstName, lastName, genericFields, 0);
    }

    public Customer applyChanges(
            CustomerChanges changes, Map<FieldName, GenericFieldDefinition> definitions) {
        String newFirstName = changes.firstName().applyTo(firstName);
        String newLastName = changes.lastName().applyTo(lastName);

        Map<FieldName, FieldValue> newGenericFields = new HashMap<>(genericFields);
        for (FieldName fieldName : changes.genericFieldsToRemove()) {
            requireDefinition(fieldName, definitions);
            newGenericFields.remove(fieldName);
        }

        for (Map.Entry<FieldName, Object> fieldToSet : changes.genericFieldsToSet().entrySet()) {
            FieldName fieldName = fieldToSet.getKey();
            GenericFieldDefinition definition = requireDefinition(fieldName, definitions);
            newGenericFields.put(fieldName, definition.parse(fieldToSet.getValue()));
        }

        return new Customer(tenantId, id, newFirstName, newLastName, newGenericFields, version);
    }

    private static GenericFieldDefinition requireDefinition(
            FieldName fieldName, Map<FieldName, GenericFieldDefinition> definitions) {
        GenericFieldDefinition definition = definitions.get(fieldName);

        if (definition == null) throw new UnknownGenericFieldException(fieldName);

        return definition;
    }
}
