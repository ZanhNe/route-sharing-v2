package com.zanh.route_sharing.utils;

public final class PaginationPolicy {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private PaginationPolicy() {
    }

    public static boolean isValid(int page, int size) {
        return page >= 0 && size >= 1 && size <= MAX_SIZE;
    }

    public static void requireValid(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size phải nằm trong khoảng từ 1 đến " + MAX_SIZE + ".");
        }
    }
}
