package com.piano.customers.infrastructure.persistence;

import com.piano.customers.domain.DuplicateGenericFieldException;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldType;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.GenericFieldDefinitionRepository;
import com.piano.customers.domain.TenantId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcGenericFieldDefinitionRepository implements GenericFieldDefinitionRepository {

    private final JdbcClient jdbc;

    public JdbcGenericFieldDefinitionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(GenericFieldDefinition definition) {
        String sql =
                """
                INSERT INTO generic_field_definition (tenant_id, name, type)
                VALUES (:tenantId, :name, :type)
                """;
        try {
            jdbc.sql(sql)
                    .param("tenantId", definition.tenantId().value())
                    .param("name", definition.name().value())
                    .param("type", definition.type().name())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateGenericFieldException(definition.name());
        }
    }

    @Override
    public Map<FieldName, GenericFieldDefinition> findByNames(
            TenantId tenantId, Set<FieldName> names) {
        if (names.isEmpty()) {
            return Map.of();
        }
        String sql =
                """
                SELECT name, type
                FROM generic_field_definition
                WHERE tenant_id = :tenantId AND name IN (:names)
                """;
        List<String> nameTexts = names.stream().map(FieldName::value).toList();

        List<GenericFieldDefinition> found =
                jdbc.sql(sql)
                        .param("tenantId", tenantId.value())
                        .param("names", nameTexts)
                        .query(
                                (row, rowNumber) ->
                                        new GenericFieldDefinition(
                                                tenantId,
                                                new FieldName(row.getString("name")),
                                                FieldType.valueOf(row.getString("type"))))
                        .list();

        Map<FieldName, GenericFieldDefinition> byName = new HashMap<>();
        for (GenericFieldDefinition definition : found) {
            byName.put(definition.name(), definition);
        }
        return byName;
    }
}
