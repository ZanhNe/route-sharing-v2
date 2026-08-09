package com.zanh.route_sharing.repository.sharedroute.driverquery.model;

import java.util.List;

public record DriverSharedRoutePageSnapshot(
        List<DriverSharedRouteSummaryRow> rows,
        long totalElements,
        int page,
        int size) {
    public DriverSharedRoutePageSnapshot {
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements không được âm.");
        }
    }
}
