package com.piano.customers.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.piano.customers.application.UpdateCustomerCommand;
import com.piano.customers.domain.Change;
import com.piano.customers.domain.CustomerChanges;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The body of {@code PATCH /customers/{id}}, kept as a JSON tree because the three states of an
 * attribute (omitted = keep, null = clear, value = set) cannot be told apart in a plain Java
 * object: omitted and null would both be null.
 */
record UpdateCustomerRequest(ObjectNode body) {

    private static final String ID = "id";
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String GENERIC_FIELDS = "generic_fields";
    private static final Set<String> ALLOWED_PROPERTIES =
            Set.of(ID, FIRST_NAME, LAST_NAME, GENERIC_FIELDS);

    /**
     * Lets Spring bind the request body straight to this type: the whole JSON object becomes {@code
     * body}.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static UpdateCustomerRequest of(ObjectNode body) {
        return new UpdateCustomerRequest(body);
    }

    UpdateCustomerCommand toCommand(TenantId tenantId, CustomerId id) {
        rejectChangedId(id);
        rejectUnknownProperties();
        GenericFieldChanges fieldChanges = readGenericFieldChanges();
        var changes =
                new CustomerChanges(
                        readNameChange(FIRST_NAME),
                        readNameChange(LAST_NAME),
                        fieldChanges.toSet(),
                        fieldChanges.toRemove());
        return new UpdateCustomerCommand(tenantId, id, changes);
    }

    /**
     * The id is immutable, but repeating the current one is accepted so a fetched customer can be
     * sent back as is.
     */
    private void rejectChangedId(CustomerId pathId) {
        JsonNode bodyId = body.get(ID);
        if (bodyId == null) {
            return;
        }
        boolean sameId = bodyId.isString() && bodyId.stringValue().equals(pathId.value());
        if (!sameId) {
            throw new InvalidRequestException("The id of a customer cannot be changed");
        }
    }

    private void rejectUnknownProperties() {
        for (Map.Entry<String, JsonNode> property : body.properties()) {
            if (!ALLOWED_PROPERTIES.contains(property.getKey())) {
                throw new InvalidRequestException("Unknown property: " + property.getKey());
            }
        }
    }

    private Change<String> readNameChange(String property) {
        JsonNode node = body.get(property);
        if (node == null) {
            return Change.keep();
        }
        if (node.isNull()) {
            return Change.clear();
        }
        if (node.isString()) {
            return Change.assign(node.stringValue());
        }
        throw new InvalidRequestException("Property " + property + " must be a string or null");
    }

    private GenericFieldChanges readGenericFieldChanges() {
        JsonNode node = body.get(GENERIC_FIELDS);
        if (node == null) {
            return GenericFieldChanges.none();
        }
        if (!node.isObject()) {
            throw new InvalidRequestException("Property generic_fields must be an object");
        }
        return GenericFieldChanges.fromJson(node.properties());
    }
}
