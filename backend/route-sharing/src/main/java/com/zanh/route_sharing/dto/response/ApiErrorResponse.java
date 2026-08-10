package com.zanh.route_sharing.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> errors,
        String referenceCode) {

    public static ApiErrorResponse of(
            Instant timestamp,
            int status,
            String code,
            String message,
            String path) {
        return new ApiErrorResponse(
                Objects.requireNonNull(timestamp, "timestamp không được trống"),
                status,
                code,
                message,
                path,
                null,
                null);
    }
}
