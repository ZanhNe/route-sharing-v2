package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

public record PassengerRideRequestQueryCriteria(
        Long actorUserId,
        TrangThaiYeuCau status,
        int page,
        int size) {

    public PassengerRideRequestQueryCriteria {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số dương.");
        }
        PaginationPolicy.requireValid(page, size);
    }

    public long offset() {
        return Math.multiplyExact((long) page, (long) size);
    }
}
