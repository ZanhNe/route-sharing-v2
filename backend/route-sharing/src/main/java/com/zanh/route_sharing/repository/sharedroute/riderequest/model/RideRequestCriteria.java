package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideRequestCriteria(
        Long actorUserId,
        Long schoolId,
        Long routeId,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        Instant now) {

    public RideRequestCriteria {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(schoolId, "schoolId");
        requirePositive(routeId, "routeId");
        Objects.requireNonNull(pickupLatitude, "pickupLatitude không được trống");
        Objects.requireNonNull(pickupLongitude, "pickupLongitude không được trống");
        Objects.requireNonNull(destinationLatitude, "destinationLatitude không được trống");
        Objects.requireNonNull(destinationLongitude, "destinationLongitude không được trống");
        Objects.requireNonNull(now, "now không được trống");
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " phải là số dương");
        }
    }
}
