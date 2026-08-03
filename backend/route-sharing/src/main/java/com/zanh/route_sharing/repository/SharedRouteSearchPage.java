package com.zanh.route_sharing.repository;

import java.util.List;

public record SharedRouteSearchPage(
        List<SharedRouteSearchRow> rows,
        long totalElements) {
    public SharedRouteSearchPage {
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }
    }
}
