package com.zanh.route_sharing.service.realtime.model;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TripLocationUpdatedRealtimeData(
        Long tripId,
        Position position,
        Instant observedAt,
        Instant receivedAt,
        BigDecimal accuracyMeters,
        Long locationSequence) {

    public TripLocationUpdatedRealtimeData {
        if (tripId == null || tripId <= 0) {
            throw new IllegalArgumentException("tripId phải là số dương.");
        }
        Objects.requireNonNull(position, "position không được trống.");
        Objects.requireNonNull(observedAt, "observedAt không được trống.");
        Objects.requireNonNull(receivedAt, "receivedAt không được trống.");
        if (accuracyMeters != null && accuracyMeters.signum() < 0) {
            throw new IllegalArgumentException("accuracyMeters không được âm.");
        }
        if (locationSequence == null || locationSequence <= 0) {
            throw new IllegalArgumentException("locationSequence phải là số dương.");
        }
    }

    public record Position(BigDecimal latitude, BigDecimal longitude) {
        public Position {
            if (!Wgs84Coordinates.isValid(latitude, longitude)) {
                throw new IllegalArgumentException("position phải là tọa độ WGS84 hợp lệ.");
            }
        }
    }
}
