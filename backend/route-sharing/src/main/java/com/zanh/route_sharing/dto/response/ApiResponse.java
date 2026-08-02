package com.zanh.route_sharing.dto.response;

import java.util.Map;

public record ApiResponse<T>(
        int status,
        T data,
        String message,
        Map<String, Object> meta) {
    public static <T> ApiResponse<T> of(int status, T data, String message, Map<String, Object> meta) {
        return new ApiResponse<>(status, data, message, meta);
    }

    public static <T> ApiResponse<T> success(int status, T data, String message) {
        return new ApiResponse<>(status, data, message, null);
    }
}