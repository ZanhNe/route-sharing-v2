package com.zanh.route_sharing.dto.trip.safety;

public record SafetyPageMeta(int page, int size, long totalElements, int totalPages) {
    public static SafetyPageMeta of(int page, int size, long totalElements) {
        long pages = totalElements == 0 ? 0 : ((totalElements - 1) / size) + 1;
        return new SafetyPageMeta(page, size, totalElements, Math.toIntExact(pages));
    }
}
