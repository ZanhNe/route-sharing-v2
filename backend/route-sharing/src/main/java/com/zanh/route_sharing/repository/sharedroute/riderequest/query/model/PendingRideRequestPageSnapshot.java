package com.zanh.route_sharing.repository.sharedroute.riderequest.query.model;

import java.util.List;
import java.util.Objects;

public record PendingRideRequestPageSnapshot(
        OwnedRouteSnapshot route,
        List<PendingRideRequestSummaryRow> rows,
        long totalElements,
        int page,
        int size) {

    public PendingRideRequestPageSnapshot {
        Objects.requireNonNull(route, "route không được trống.");
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements phải >= 0");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page phải >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size phải >= 1");
        }
    }
}
