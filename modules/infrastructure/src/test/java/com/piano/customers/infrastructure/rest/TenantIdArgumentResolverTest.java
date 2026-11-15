package com.piano.customers.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.DomainValidationException;
import com.piano.customers.domain.TenantId;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.context.request.ServletWebRequest;

class TenantIdArgumentResolverTest {

    private final TenantIdArgumentResolver resolver = new TenantIdArgumentResolver();

    @SuppressWarnings("unused")
    private void handler(TenantId tenantId, String other) {}

    private static MethodParameter parameter(int index) throws NoSuchMethodException {
        Method handler =
                TenantIdArgumentResolverTest.class.getDeclaredMethod(
                        "handler", TenantId.class, String.class);
        return new MethodParameter(handler, index);
    }

    private static ServletWebRequest requestWithTenant(String tenant) {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", tenant);
        return new ServletWebRequest(request);
    }

    @Test
    void supportsOnlyTenantIdParameters() throws Exception {
        assertThat(resolver.supportsParameter(parameter(0))).isTrue();
        assertThat(resolver.supportsParameter(parameter(1))).isFalse();
    }

    @Test
    void readsTheTenantIdFromTheHeader() throws Exception {
        TenantId tenantId =
                resolver.resolveArgument(parameter(0), null, requestWithTenant("tenant_123"), null);

        assertThat(tenantId).isEqualTo(new TenantId("tenant_123"));
    }

    @Test
    void rejectsAMissingHeader() {
        var withoutHeader = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, withoutHeader, null))
                .isInstanceOf(MissingRequestHeaderException.class)
                .hasMessageContaining("X-Tenant-Id");
    }

    @Test
    void rejectsABlankHeader() {
        assertThatThrownBy(
                        () ->
                                resolver.resolveArgument(
                                        parameter(0), null, requestWithTenant(" "), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage(
                        "Tenant id must be 1-128 characters: letters, digits, underscore or hyphen");
    }
}
