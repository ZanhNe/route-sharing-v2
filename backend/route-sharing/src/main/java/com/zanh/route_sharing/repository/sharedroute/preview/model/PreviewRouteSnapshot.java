package com.zanh.route_sharing.repository.sharedroute.preview.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PreviewRouteSnapshot(
        Long routeId,
        Long routeVersion,
        TrangThaiLoTrinh status,
        PreviewGeoPoint origin,
        PreviewGeoPoint driverDestination,
        String originalRouteGeoJson,
        BigDecimal originalDistanceMeters,
        long originalDurationSeconds,
        Instant expectedDepartureTime,
        Integer remainingSeats,
        BigDecimal suggestedSupportPerKm) {

    public PreviewRouteSnapshot {
        Objects.requireNonNull(routeId, "routeId không được trống");
        Objects.requireNonNull(routeVersion, "routeVersion không được trống");
        Objects.requireNonNull(status, "status không được trống");
        Objects.requireNonNull(origin, "origin không được trống");
        Objects.requireNonNull(driverDestination, "driverDestination không được trống");
        Objects.requireNonNull(originalRouteGeoJson, "originalRouteGeoJson không được trống");
        Objects.requireNonNull(originalDistanceMeters, "originalDistanceMeters không được trống");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống");
        Objects.requireNonNull(remainingSeats, "remainingSeats không được trống");
    }
}
