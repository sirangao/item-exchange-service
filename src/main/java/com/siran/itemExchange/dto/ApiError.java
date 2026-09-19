package com.siran.itemExchange.dto;

import lombok.Getter;

/**
 * The single error shape every failing endpoint returns: {"error": "..."}.
 * The frontend reads err.response.data.error, so keep the field name as-is.
 */
@Getter
public class ApiError {

    private final String error;

    public ApiError(String error) {
        this.error = error;
    }
}
