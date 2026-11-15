package com.piano.customers.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.DuplicateGenericFieldException;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldType;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JdbcGenericFieldDefinitionRepository.class)
class JdbcGenericFieldDefinitionRepositoryIT extends PostgresRepositoryTest {

    private static final TenantId TENANT = new TenantId("tenant_123");
    private static final TenantId OTHER_TENANT = new TenantId("tenant_456");
    private static final FieldName DOB = new FieldName("dob");
    private static final FieldName VIP = new FieldName("vip");
    private static final FieldName SCORE = new FieldName("score");

    @Autowired private JdbcGenericFieldDefinitionRepository repository;

    @Test
    void storesAndFindsADefinitionOfEveryType() {
        for (FieldType type : FieldType.values()) {
            FieldName name = new FieldName("field_" + type.name().toLowerCase());
            repository.insert(new GenericFieldDefinition(TENANT, name, type));

            Map<FieldName, GenericFieldDefinition> found =
                    repository.findByNames(TENANT, Set.of(name));

            assertThat(found).containsEntry(name, new GenericFieldDefinition(TENANT, name, type));
        }
    }

    @Test
    void findsOnlyTheRequestedNames() {
        repository.insert(new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME));
        repository.insert(new GenericFieldDefinition(TENANT, VIP, FieldType.BOOL));
        repository.insert(new GenericFieldDefinition(TENANT, SCORE, FieldType.INTEGER));

        Map<FieldName, GenericFieldDefinition> found =
                repository.findByNames(TENANT, Set.of(DOB, SCORE));

        assertThat(found).containsOnlyKeys(DOB, SCORE);
    }

    @Test
    void leavesOutNamesThatAreNotRegistered() {
        repository.insert(new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME));

        Map<FieldName, GenericFieldDefinition> found =
                repository.findByNames(TENANT, Set.of(DOB, VIP));

        assertThat(found).containsOnlyKeys(DOB);
    }

    @Test
    void findsNothingForAnEmptySetOfNames() {
        repository.insert(new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME));

        assertThat(repository.findByNames(TENANT, Set.of())).isEmpty();
    }

    @Test
    void rejectsTheSameNameTwiceForOneTenant() {
        repository.insert(new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME));

        assertThatThrownBy(
                        () ->
                                repository.insert(
                                        new GenericFieldDefinition(TENANT, DOB, FieldType.STRING)))
                .isInstanceOf(DuplicateGenericFieldException.class);
        assertThat(repository.findByNames(TENANT, Set.of(DOB)).get(DOB).type())
                .isEqualTo(FieldType.DATETIME);
    }

    @Test
    void keepsTenantsApart() {
        repository.insert(new GenericFieldDefinition(TENANT, DOB, FieldType.DATETIME));
        repository.insert(new GenericFieldDefinition(OTHER_TENANT, DOB, FieldType.STRING));

        assertThat(repository.findByNames(TENANT, Set.of(DOB)).get(DOB).type())
                .isEqualTo(FieldType.DATETIME);
        assertThat(repository.findByNames(OTHER_TENANT, Set.of(DOB)).get(DOB).type())
                .isEqualTo(FieldType.STRING);
    }

    @Test
    void doesNotSeeDefinitionsOfAnotherTenant() {
        repository.insert(new GenericFieldDefinition(OTHER_TENANT, DOB, FieldType.DATETIME));

        assertThat(repository.findByNames(TENANT, Set.of(DOB))).isEmpty();
    }
}
