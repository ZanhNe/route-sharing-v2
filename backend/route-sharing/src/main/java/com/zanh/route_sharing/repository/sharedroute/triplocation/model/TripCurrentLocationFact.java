package com.zanh.route_sharing.repository.sharedroute.triplocation.model;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TripCurrentLocationFact(
        Long tripId,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant observedAt,
        Instant receivedAt,
        BigDecimal accuracyMeters,
        Long locationSequence) {

    public TripCurrentLocationFact {
        if (tripId == null || tripId <= 0) {
            throw new IllegalArgumentException("tripId phải là số dương.");
        }
        if (!Wgs84Coordinates.isValid(latitude, longitude)) {
            throw new IllegalArgumentException("Current location phải là tọa độ WGS84 hợp lệ.");
        }
        Objects.requireNonNull(observedAt, "observedAt không được trống.");
        Objects.requireNonNull(receivedAt, "receivedAt không được trống.");
        if (accuracyMeters != null && accuracyMeters.signum() < 0) {
            throw new IllegalArgumentException("accuracyMeters không được âm.");
        }
        if (locationSequence == null || locationSequence <= 0) {
            throw new IllegalArgumentException("locationSequence phải là số dương.");
        }
    }
}
