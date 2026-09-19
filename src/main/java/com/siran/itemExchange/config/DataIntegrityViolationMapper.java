package com.siran.itemExchange.config;

import com.siran.itemExchange.dto.ApiError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Backstop for the unique constraints on users.username / users.email and for enum
 * values the database rejects. The resources check the common cases up front; this
 * keeps anything that slips past from surfacing as a stack-trace 500.
 */
public class DataIntegrityViolationMapper implements ExceptionMapper<DataIntegrityViolationException> {

    @Override
    public Response toResponse(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause().getMessage();
        String message = cause != null && cause.contains("Duplicate entry")
                ? "That value is already taken"
                : "The request conflicts with what is already stored";

        return Response.status(Response.Status.CONFLICT)
                .entity(new ApiError(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
