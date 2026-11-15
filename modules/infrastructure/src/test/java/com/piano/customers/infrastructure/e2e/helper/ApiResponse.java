package com.piano.customers.infrastructure.e2e.helper;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;

/**
 * What a test sees of an HTTP response: status, headers and the JSON body parsed into a plain map.
 */
public record ApiResponse(int status, HttpHeaders headers, @Nullable Map<String, Object> body) {}
