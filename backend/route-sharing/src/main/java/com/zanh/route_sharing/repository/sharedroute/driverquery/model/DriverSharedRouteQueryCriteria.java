package com.zanh.route_sharing.repository.sharedroute.driverquery.model;

import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

public record DriverSharedRouteQueryCriteria(
        Long actorUserId,
        TrangThaiLoTrinh status,
        int page,
        int size) {

    public DriverSharedRouteQueryCriteria {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số dương.");
        }
        PaginationPolicy.requireValid(page, size);
    }

    public long offset() {
        return Math.multiplyExact((long) page, (long) size);
    }
}
