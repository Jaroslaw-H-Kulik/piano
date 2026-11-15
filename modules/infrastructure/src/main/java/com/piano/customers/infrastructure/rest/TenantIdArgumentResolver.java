package com.piano.customers.infrastructure.rest;

import com.piano.customers.domain.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lets a handler method take a {@link TenantId} parameter, read from the {@value #TENANT_ID_HEADER}
 * header. A missing header is a {@link MissingRequestHeaderException} and an invalid one a domain
 * validation error, both 400.
 */
class TenantIdArgumentResolver implements HandlerMethodArgumentResolver {

    static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(TenantId.class);
    }

    @Override
    public TenantId resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory)
            throws MissingRequestHeaderException {
        String header = webRequest.getHeader(TENANT_ID_HEADER);
        if (header == null) {
            throw new MissingRequestHeaderException(TENANT_ID_HEADER, parameter);
        }
        return new TenantId(header);
    }
}
