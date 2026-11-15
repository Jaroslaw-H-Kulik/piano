package com.piano.customers.infrastructure.rest;

import com.piano.customers.domain.CustomerModifiedConcurrentlyException;
import com.piano.customers.domain.CustomerNotFoundException;
import com.piano.customers.domain.DomainValidationException;
import com.piano.customers.domain.DuplicateCustomerException;
import com.piano.customers.domain.DuplicateGenericFieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({DomainValidationException.class, InvalidRequestException.class})
    ResponseEntity<ErrorResponse> handleInvalidRequest(RuntimeException e) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof UnrecognizedPropertyException unknownProperty) {
            return respond(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST",
                    "Unknown property: " + unknownProperty.getPropertyName());
        }
        if (e.getCause() instanceof MismatchedInputException wrongType
                && !wrongType.getPath().isEmpty()) {
            String property = wrongType.getPath().getFirst().getPropertyName();
            return respond(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST",
                    "Property " + property + " has the wrong type");
        }
        return respond(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "The request body must be a valid JSON object without duplicate properties");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        return respond(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Header " + e.getHeaderName() + " is required");
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(DuplicateCustomerException.class)
    ResponseEntity<ErrorResponse> handleDuplicateCustomer(DuplicateCustomerException e) {
        return respond(HttpStatus.CONFLICT, "CUSTOMER_ALREADY_EXISTS", e.getMessage());
    }

    @ExceptionHandler(DuplicateGenericFieldException.class)
    ResponseEntity<ErrorResponse> handleDuplicateGenericField(DuplicateGenericFieldException e) {
        return respond(HttpStatus.CONFLICT, "GENERIC_FIELD_ALREADY_EXISTS", e.getMessage());
    }

    @ExceptionHandler(CustomerModifiedConcurrentlyException.class)
    ResponseEntity<ErrorResponse> handleConcurrentModification(
            CustomerModifiedConcurrentlyException e) {
        return respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleUnknownPath(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such endpoint");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException e) {
        return respond(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "Method " + e.getMethod() + " is not supported here");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleWrongContentType(HttpMediaTypeNotSupportedException e) {
        return respond(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content type must be application/json");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        LOG.error("Unexpected error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    private static ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
