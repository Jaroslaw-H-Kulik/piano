package com.piano.customers.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.Change;
import com.piano.customers.domain.CustomerChanges;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.TenantId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class UpdateCustomerRequestTest {

    private static final TenantId TENANT = new TenantId("tenant_123");
    private static final CustomerId CUSTOMER_ID = new CustomerId("cus_123");

    private final JsonMapper mapper = JsonMapper.builder().build();

    private CustomerChanges toChanges(String body) {
        var request = new UpdateCustomerRequest((ObjectNode) mapper.readTree(body));
        return request.toCommand(TENANT, CUSTOMER_ID).changes();
    }

    @Test
    void omittedAttributesAreKept() {
        CustomerChanges changes = toChanges("{}");

        assertThat(changes.firstName()).isEqualTo(Change.keep());
        assertThat(changes.lastName()).isEqualTo(Change.keep());
        assertThat(changes.genericFieldNames()).isEmpty();
    }

    @Test
    void nullAttributesAreCleared() {
        CustomerChanges changes =
                toChanges("{\"first_name\":null,\"generic_fields\":{\"dob\":null}}");

        assertThat(changes.firstName()).isEqualTo(Change.clear());
        assertThat(changes.lastName()).isEqualTo(Change.keep());
        assertThat(changes.genericFieldsToRemove()).containsOnly(new FieldName("dob"));
        assertThat(changes.genericFieldsToSet()).isEmpty();
    }

    @Test
    void valuesAreAssigned() {
        CustomerChanges changes =
                toChanges("{\"last_name\":\"Bull\",\"generic_fields\":{\"vip\":false}}");

        assertThat(changes.lastName()).isEqualTo(Change.assign("Bull"));
        assertThat(changes.genericFieldsToSet())
                .containsOnly(Map.entry(new FieldName("vip"), false));
        assertThat(changes.genericFieldsToRemove()).isEmpty();
    }

    @Test
    void acceptsTheCurrentIdInTheBody() {
        CustomerChanges changes = toChanges("{\"id\":\"cus_123\"}");

        assertThat(changes.firstName()).isEqualTo(Change.keep());
        assertThat(changes.genericFieldNames()).isEmpty();
    }

    @Test
    void rejectsADifferentIdInTheBody() {
        assertThatThrownBy(() -> toChanges("{\"id\":\"other\"}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("The id of a customer cannot be changed");
    }

    @Test
    void rejectsAnIdThatIsNotAString() {
        assertThatThrownBy(() -> toChanges("{\"id\":null}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("The id of a customer cannot be changed");
    }

    @Test
    void rejectsUnknownProperties() {
        assertThatThrownBy(() -> toChanges("{\"nick\":\"x\"}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Unknown property: nick");
    }

    @Test
    void rejectsANameThatIsNotAStringOrNull() {
        assertThatThrownBy(() -> toChanges("{\"first_name\":5}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Property first_name must be a string or null");
    }

    @Test
    void rejectsGenericFieldsThatAreNotAnObject() {
        assertThatThrownBy(() -> toChanges("{\"generic_fields\":[1]}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Property generic_fields must be an object");
    }

    @Test
    void rejectsGenericFieldsSentAsNull() {
        assertThatThrownBy(() -> toChanges("{\"generic_fields\":null}"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
