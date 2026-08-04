package com.zanh.route_sharing.repository.sharedroute.search.model;

import java.math.BigDecimal;

public record SharedRouteSearchContext(
        BigDecimal sameDestinationRadiusMeters,
        BigDecimal destinationNearRouteRadiusMeters,
        BigDecimal maxPickupDeviationMeters,
        int departureToleranceMinutes) {
    public SharedRouteSearchContext {
        requirePositive(sameDestinationRadiusMeters, "sameDestinationRadiusMeters");
        requirePositive(destinationNearRouteRadiusMeters, "destinationNearRouteRadiusMeters");

        if (maxPickupDeviationMeters == null || maxPickupDeviationMeters.signum() < 0) {
            throw new IllegalArgumentException("maxPickupDeviationMeters phải >= 0");
        }
        if (departureToleranceMinutes < 0) {
            throw new IllegalArgumentException("departureToleranceMinutes phải >= 0");
        }
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " phải > 0");
        }
    }
}
