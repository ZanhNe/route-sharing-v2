package com.zanh.route_sharing.service.riderequest.model;

import java.math.BigDecimal;
import java.util.Objects;

public record PickupDeviation(
        BigDecimal distanceMeters,
        long durationSeconds) {

    public PickupDeviation {
        Objects.requireNonNull(distanceMeters, "distanceMeters không được trống");
        if (distanceMeters.signum() < 0 || durationSeconds < 0) {
            throw new IllegalArgumentException("Độ lệch đón không được âm");
        }
    }

    public static PickupDeviation zero() {
        return new PickupDeviation(BigDecimal.ZERO, 0L);
    }
}
