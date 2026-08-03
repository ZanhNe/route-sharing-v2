package com.zanh.route_sharing.repository;

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
        LocalDate membershipDate,
        Instant departureFrom,
        Instant departureTo,

        SharedRouteSearchContext context,
        int page,
        int size) {
    public SharedRouteSearchCriteria {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId must be positive");
        }
        if (schoolId == null || schoolId <= 0) {
            throw new IllegalArgumentException("schoolId must be positive");
        }
        if (pickupLatitude == null || pickupLongitude == null
                || destinationLatitude == null || destinationLongitude == null) {
            throw new IllegalArgumentException("Search coordinates are required");
        }
        if (now == null || membershipDate == null || departureFrom == null || departureTo == null) {
            throw new IllegalArgumentException("Search time values are required");
        }
        if (departureTo.isBefore(departureFrom)) {
            throw new IllegalArgumentException("departureTo must not be before departureFrom");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Invalid page/size");
        }
    }

    public long offset() {
        return Math.multiplyExact((long) page, size);
    }
}
