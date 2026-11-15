package com.piano.customers.infrastructure.persistence;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.CustomerModifiedConcurrentlyException;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.DuplicateCustomerException;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldValue;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.GenericFieldDefinitionRepository;
import com.piano.customers.domain.TenantId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcCustomerRepository implements CustomerRepository {

    private final JdbcClient jdbc;
    private final GenericFieldDefinitionRepository definitionRepository;

    public JdbcCustomerRepository(
            JdbcClient jdbc, GenericFieldDefinitionRepository definitionRepository) {
        this.jdbc = jdbc;
        this.definitionRepository = definitionRepository;
    }

    @Override
    public void insert(Customer customer) {
        String sql =
                """
                INSERT INTO customer (tenant_id, public_id, first_name, last_name, generic_fields, version)
                VALUES (:tenantId, :publicId, :firstName, :lastName, CAST(:genericFields AS jsonb), :version)
                """;
        try {
            jdbc.sql(sql)
                    .param("tenantId", customer.tenantId().value())
                    .param("publicId", customer.id().value())
                    .param("firstName", customer.firstName())
                    .param("lastName", customer.lastName())
                    .param("genericFields", GenericFieldsJson.toJson(customer.genericFields()))
                    .param("version", customer.version())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCustomerException(customer.id());
        }
    }

    @Override
    public Optional<Customer> findById(TenantId tenantId, CustomerId id) {
        String sql =
                """
                SELECT first_name, last_name, CAST(generic_fields AS text) AS generic_fields, version
                FROM customer
                WHERE tenant_id = :tenantId AND public_id = :publicId
                """;
        Optional<StoredCustomer> stored =
                jdbc.sql(sql)
                        .param("tenantId", tenantId.value())
                        .param("publicId", id.value())
                        .query(
                                (row, rowNumber) ->
                                        new StoredCustomer(
                                                row.getString("first_name"),
                                                row.getString("last_name"),
                                                row.getString("generic_fields"),
                                                row.getLong("version")))
                        .optional();

        return stored.map(row -> toCustomer(tenantId, id, row));
    }

    @Override
    public Customer update(Customer customer) {
        String sql =
                """
                UPDATE customer
                SET first_name = :firstName,
                    last_name = :lastName,
                    generic_fields = CAST(:genericFields AS jsonb),
                    version = version + 1
                WHERE tenant_id = :tenantId AND public_id = :publicId AND version = :version
                """;
        int updatedRows =
                jdbc.sql(sql)
                        .param("tenantId", customer.tenantId().value())
                        .param("publicId", customer.id().value())
                        .param("firstName", customer.firstName())
                        .param("lastName", customer.lastName())
                        .param("genericFields", GenericFieldsJson.toJson(customer.genericFields()))
                        .param("version", customer.version())
                        .update();

        if (updatedRows == 0) {
            // The row was changed or deleted since the caller read it. Which of the two is for the
            // use case to
            // find out (by reading again), so there is no "not found" here.
            throw new CustomerModifiedConcurrentlyException(customer.id());
        }
        return new Customer(
                customer.tenantId(),
                customer.id(),
                customer.firstName(),
                customer.lastName(),
                customer.genericFields(),
                customer.version() + 1);
    }

    @Override
    public boolean delete(TenantId tenantId, CustomerId id) {
        String sql = "DELETE FROM customer WHERE tenant_id = :tenantId AND public_id = :publicId";
        int deletedRows =
                jdbc.sql(sql)
                        .param("tenantId", tenantId.value())
                        .param("publicId", id.value())
                        .update();
        return deletedRows > 0;
    }

    private Customer toCustomer(TenantId tenantId, CustomerId id, StoredCustomer stored) {
        Map<FieldName, FieldValue> genericFields =
                parseGenericFields(tenantId, stored.genericFieldsJson());
        return new Customer(
                tenantId,
                id,
                stored.firstName(),
                stored.lastName(),
                genericFields,
                stored.version());
    }

    private Map<FieldName, FieldValue> parseGenericFields(
            TenantId tenantId, String genericFieldsJson) {
        Map<FieldName, Object> rawValues = GenericFieldsJson.fromJson(genericFieldsJson);
        Map<FieldName, GenericFieldDefinition> fieldDefinitions =
                definitionRepository.findByNames(tenantId, rawValues.keySet());

        Map<FieldName, FieldValue> typedValues = new HashMap<>();
        for (Map.Entry<FieldName, Object> rawValue : rawValues.entrySet()) {
            GenericFieldDefinition definition = fieldDefinitions.get(rawValue.getKey());
            if (definition == null) {
                throw new IllegalStateException(
                        "Stored generic field "
                                + rawValue.getKey().value()
                                + " has no definition for tenant "
                                + tenantId.value());
            }
            typedValues.put(rawValue.getKey(), definition.parse(rawValue.getValue()));
        }
        return typedValues;
    }

    private record StoredCustomer(
            @Nullable String firstName,
            @Nullable String lastName,
            String genericFieldsJson,
            long version) {}
}
