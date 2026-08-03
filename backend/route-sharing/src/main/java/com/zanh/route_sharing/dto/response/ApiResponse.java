package com.zanh.route_sharing.dto.response;

public record ApiResponse<T>(
        int status,
        T data,
        String message,
        PageMeta meta) {
    public static <T> ApiResponse<T> of(int status, T data, String message, PageMeta meta) {
        return new ApiResponse<>(status, data, message, meta);
    }

    public static <T> ApiResponse<T> success(int status, T data, String message) {
        return new ApiResponse<>(status, data, message, null);
    }
}