package com.zanh.route_sharing.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, String> errors;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errors = null;
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.errors = null;
    }

    public BusinessException(HttpStatus status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}