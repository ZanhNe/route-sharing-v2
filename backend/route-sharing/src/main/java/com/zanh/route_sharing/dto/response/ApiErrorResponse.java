package com.zanh.route_sharing.dto.response;

import java.util.Map;

import lombok.Builder;

@Builder
public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        Object meta) {
}
