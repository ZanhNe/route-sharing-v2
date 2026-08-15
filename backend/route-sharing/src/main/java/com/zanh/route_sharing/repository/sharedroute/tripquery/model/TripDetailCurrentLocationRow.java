package com.zanh.route_sharing.repository.sharedroute.tripquery.model;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TripDetailCurrentLocationRow(
        BigDecimal latitude,
        BigDecimal longitude,
        Instant observedAt,
        Instant receivedAt,
        BigDecimal accuracyMeters,
        Long locationSequence) {

    public TripDetailCurrentLocationRow {
        if (!Wgs84Coordinates.isValid(latitude, longitude)) {
            throw new IllegalArgumentException("Current Driver location phải là WGS84 hợp lệ.");
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
