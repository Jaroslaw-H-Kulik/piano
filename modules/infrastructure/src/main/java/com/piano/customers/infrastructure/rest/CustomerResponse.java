package com.piano.customers.infrastructure.rest;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldValue;
import java.util.HashMap;
import java.util.Map;

public record CustomerResponse(
        String id, String firstName, String lastName, Map<String, Object> genericFields) {

    public static CustomerResponse from(Customer customer) {
        Map<String, Object> genericFields = new HashMap<>();
        for (Map.Entry<FieldName, FieldValue> field : customer.genericFields().entrySet()) {
            genericFields.put(field.getKey().value(), field.getValue().rawValue());
        }
        return new CustomerResponse(
                customer.id().value(), customer.firstName(), customer.lastName(), genericFields);
    }
}
