package com.zanh.route_sharing.dto.response;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
    public static PageMeta of(int page, int size, long totalElements) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }

        long pages = totalElements == 0 ? 0 : ((totalElements - 1) / size) + 1;
        int totalPages = Math.toIntExact(pages);
        return new PageMeta(
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1);
    }
}
