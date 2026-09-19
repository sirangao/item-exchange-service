package com.siran.itemExchange.config;

import com.siran.itemExchange.dto.ApiError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Turns a @Valid failure into 400 {"error": "title: must not be blank"} instead of
 * Jersey's default empty body, which the frontend has no way to display.
 */
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolationMapper::describe)
                .collect(Collectors.joining("; "));

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError(message.isEmpty() ? "Invalid request" : message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /** "listingType: must be one of: sell, exchange, both" — the leaf name, not the full JAX-RS path. */
    private static String describe(ConstraintViolation<?> violation) {
        String field = StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .map(Path.Node::getName)
                .reduce((first, last) -> last)
                .orElse("request");
        return field + ": " + violation.getMessage();
    }
}
