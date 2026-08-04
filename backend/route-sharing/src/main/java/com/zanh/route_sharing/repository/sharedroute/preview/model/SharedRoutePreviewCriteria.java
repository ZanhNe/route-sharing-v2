package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SharedRoutePreviewCriteria(
        Long actorUserId,
        Long schoolId,
        Long sharedRouteId,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        Instant now) {

    public SharedRoutePreviewCriteria {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số nguyên dương");
        }
        if (schoolId == null || schoolId <= 0) {
            throw new IllegalArgumentException("schoolId phải là số nguyên dương");
        }
        if (sharedRouteId == null || sharedRouteId <= 0) {
            throw new IllegalArgumentException("sharedRouteId phải là số nguyên dương");
        }
        Objects.requireNonNull(pickupLatitude, "pickupLatitude không được trống");
        Objects.requireNonNull(pickupLongitude, "pickupLongitude không được trống");
        Objects.requireNonNull(destinationLatitude, "destinationLatitude không được trống");
        Objects.requireNonNull(destinationLongitude, "destinationLongitude không được trống");
        Objects.requireNonNull(now, "now không được trống");
    }
}
