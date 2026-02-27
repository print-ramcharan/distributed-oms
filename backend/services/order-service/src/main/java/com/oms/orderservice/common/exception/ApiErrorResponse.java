package com.oms.orderservice.common.exception;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class ApiErrorResponse {
    private final int status;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> errors;

    public ApiErrorResponse(int status, String message, Map<String, String> errors){
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now();
        this.errors = errors;
    }

    public static ApiErrorResponse of(int status, String message, Map<String, String> errors){
        return new ApiErrorResponse(status, message, errors);
    }
}
