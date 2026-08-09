package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

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
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size phải nằm trong khoảng từ 1 đến 50.");
        }
    }

    public long offset() {
        return Math.multiplyExact((long) page, (long) size);
    }
}
