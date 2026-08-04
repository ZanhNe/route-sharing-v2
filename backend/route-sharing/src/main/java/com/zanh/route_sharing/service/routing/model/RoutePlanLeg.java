package com.zanh.route_sharing.service.routing.model;

import java.math.BigDecimal;
import java.util.Objects;

public record RoutePlanLeg(
        int sequence,
        RouteWaypointRole fromRole,
        RouteWaypointRole toRole,
        BigDecimal distanceMeters,
        long durationSeconds,
        boolean collapsed) {

    public RoutePlanLeg {
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence không được trống");
        }
        Objects.requireNonNull(fromRole, "fromRole không được trống");
        Objects.requireNonNull(toRole, "toRole không được trống");
        Objects.requireNonNull(distanceMeters, "distanceMeters không được trống");
        if (distanceMeters.signum() < 0 || durationSeconds < 0) {
            throw new IllegalArgumentException("Leg metrics không được âm");
        }
        if (collapsed && (distanceMeters.signum() != 0 || durationSeconds != 0)) {
            throw new IllegalArgumentException("Collapsed leg metrics phải bằng 0");
        }
    }
}
