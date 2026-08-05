package com.zanh.route_sharing.domain.riderequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

public record RideRequestPolicySnapshot(
        Long configurationId,
        Long configurationVersion,
        BigDecimal sameDestinationRadiusMeters,
        BigDecimal destinationNearRouteRadiusMeters,
        BigDecimal maxPickupDeviationMeters,
        long maxPickupDeviationSeconds,
        BigDecimal minimumConvenienceRatioPercent,
        Duration requestTtl,
        Duration bookingCutoff,
        Duration rejectionCooldown) {

    public RideRequestPolicySnapshot {
        requirePositive(configurationId, "configurationId");
        requireNonNegative(configurationVersion, "configurationVersion");
        requirePositive(sameDestinationRadiusMeters, "sameDestinationRadiusMeters");
        requirePositive(destinationNearRouteRadiusMeters, "destinationNearRouteRadiusMeters");
        requireNonNegative(maxPickupDeviationMeters, "maxPickupDeviationMeters");
        if (maxPickupDeviationSeconds < 0) {
            throw new IllegalArgumentException("maxPickupDeviationSeconds không được âm");
        }
        Objects.requireNonNull(minimumConvenienceRatioPercent,
                "minimumConvenienceRatioPercent không được trống");
        if (minimumConvenienceRatioPercent.signum() < 0
                || minimumConvenienceRatioPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("minimumConvenienceRatioPercent phải trong đoạn 0..100");
        }
        requirePositive(requestTtl, "requestTtl");
        requireNonNegative(bookingCutoff, "bookingCutoff");
        requireNonNegative(rejectionCooldown, "rejectionCooldown");
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " phải là số dương");
        }
    }

    private static void requireNonNegative(Long value, String name) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(name + " không được âm");
        }
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " phải lớn hơn 0");
        }
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + " không được âm");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " không được trống");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " phải lớn hơn 0");
        }
    }

    private static void requireNonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name + " không được trống");
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " không được âm");
        }
    }
}
