package com.zanh.route_sharing.service.routing;

import java.math.BigDecimal;
import java.util.Objects;

public record RoutePlanningPolicy(
        BigDecimal duplicateWaypointToleranceMeters,
        BigDecimal waypointSnapToleranceMeters) {

    public RoutePlanningPolicy {
        Objects.requireNonNull(duplicateWaypointToleranceMeters, "duplicateWaypointToleranceMeters");
        Objects.requireNonNull(waypointSnapToleranceMeters, "waypointSnapToleranceMeters");
        if (duplicateWaypointToleranceMeters.signum() < 0) {
            throw new IllegalArgumentException("duplicateWaypointToleranceMeters không được âm");
        }
        if (waypointSnapToleranceMeters.signum() <= 0) {
            throw new IllegalArgumentException("waypointSnapToleranceMeters phải lớn hơn 0");
        }
        if (duplicateWaypointToleranceMeters.compareTo(waypointSnapToleranceMeters) > 0) {
            throw new IllegalArgumentException(
                    "duplicateWaypointToleranceMeters phải nhỏ hơn hoặc bằng waypointSnapToleranceMeters");
        }
    }
}
