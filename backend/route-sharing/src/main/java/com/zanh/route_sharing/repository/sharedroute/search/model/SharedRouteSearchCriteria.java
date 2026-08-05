package com.zanh.route_sharing.repository.sharedroute.search.model;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SharedRouteSearchCriteria(
        Long actorUserId,
        Long schoolId,

        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,

        Instant now,
        LocalDate requestedTravelDate,
        Instant departureFrom,
        Instant departureTo,

        SharedRouteMatchingContext context,
        int page,
        int size) {

    public SharedRouteSearchCriteria {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số dương");
        }
        if (schoolId == null || schoolId <= 0) {
            throw new IllegalArgumentException("schoolId phải là số dương");
        }
        if (pickupLatitude == null || pickupLongitude == null
                || destinationLatitude == null || destinationLongitude == null) {
            throw new IllegalArgumentException("Tọa độ tìm kiếm là bắt buộc");
        }
        if (now == null
                || requestedTravelDate == null
                || departureFrom == null
                || departureTo == null) {
            throw new IllegalArgumentException("Các giá trị thời gian tìm kiếm là bắt buộc");
        }
        if (departureTo.isBefore(departureFrom)) {
            throw new IllegalArgumentException("departureTo phải không được trước departureFrom");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context là bắt buộc");
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Số trang hoặc số lượng bản ghi không hợp lệ");
        }
    }

    public long offset() {
        return Math.multiplyExact((long) page, size);
    }
}
