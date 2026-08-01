package com.zanh.route_sharing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> errors;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", message, null);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, "BUSINESS_ERROR", message, null);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessException(HttpStatus status, String code, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors == null ? null : Map.copyOf(errors);
    }
}
