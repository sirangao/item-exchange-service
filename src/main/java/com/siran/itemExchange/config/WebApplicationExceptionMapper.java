package com.siran.itemExchange.config;

import com.siran.itemExchange.dto.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Keeps the status a resource chose (e.g. BadRequestException from an unknown userId)
 * but gives it the same {"error": "..."} body as everything else.
 */
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        Response original = e.getResponse();

        // 404/405 and friends are routing outcomes with no message worth forwarding.
        String message = e.getMessage() == null ? original.getStatusInfo().getReasonPhrase() : e.getMessage();

        return Response.status(original.getStatus())
                .entity(new ApiError(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
