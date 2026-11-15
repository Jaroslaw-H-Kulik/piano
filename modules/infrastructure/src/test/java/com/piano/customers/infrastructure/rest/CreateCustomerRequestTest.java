package com.piano.customers.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.application.CreateCustomerCommand;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.DomainValidationException;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class CreateCustomerRequestTest {

    private static final TenantId TENANT = new TenantId("tenant_123");

    private final JsonMapper mapper =
            JsonMapper.builder()
                    .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .build();

    private CreateCustomerCommand toCommand(String body) {
        return mapper.readValue(body, CreateCustomerRequest.class).toCommand(TENANT);
    }

    @Test
    void convertsTheRequestFromTheTaskExample() {
        CreateCustomerCommand command =
                toCommand(
                        """
                {"id":"cus_123","first_name":"John","last_name":"Smith",
                 "generic_fields":{"dob":"1990-01-01T00:00:00","vip":true,"score":100}}""");

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.id()).isEqualTo(new CustomerId("cus_123"));
        assertThat(command.firstName()).isEqualTo("John");
        assertThat(command.lastName()).isEqualTo("Smith");
        assertThat(command.genericFields())
                .containsOnly(
                        Map.entry(new FieldName("dob"), "1990-01-01T00:00:00"),
                        Map.entry(new FieldName("vip"), true),
                        Map.entry(new FieldName("score"), 100L));
    }

    @Test
    void needsOnlyAnId() {
        CreateCustomerCommand command = toCommand("{\"id\":\"cus_123\"}");

        assertThat(command.firstName()).isNull();
        assertThat(command.lastName()).isNull();
        assertThat(command.genericFields()).isEmpty();
    }

    @Test
    void rejectsANullGenericFieldBecauseThereIsNothingToRemove() {
        assertThatThrownBy(
                        () -> toCommand("{\"id\":\"cus_123\",\"generic_fields\":{\"dob\":null}}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Field dob must not be null when creating a customer; omit it instead");
    }

    @Test
    void rejectsAMissingId() {
        assertThatThrownBy(() -> toCommand("{\"first_name\":\"John\"}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Property id is required and must be a string");
    }

    @Test
    void rejectsABlankId() {
        assertThatThrownBy(() -> toCommand("{\"id\":\"  \"}"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsAnInvalidFieldValue() {
        assertThatThrownBy(
                        () -> toCommand("{\"id\":\"cus_123\",\"generic_fields\":{\"score\":1.5}}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Field score must be a boolean, an integer, a string or null");
    }
}
