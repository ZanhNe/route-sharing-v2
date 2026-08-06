package com.zanh.route_sharing.repository.sharedroute.riderequest.query.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record OwnedRouteSnapshot(
        Long routeId,
        TrangThaiLoTrinh routeStatus,
        Instant expectedDepartureTime,
        Integer offeredSeats,
        Integer remainingSeats,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        String originAddress,
        BigDecimal driverDestinationLatitude,
        BigDecimal driverDestinationLongitude,
        String driverDestinationAddress,
        String originalRouteGeoJson,
        BigDecimal originalDistanceMeters,
        Long originalDurationSeconds) {

    public OwnedRouteSnapshot {
        Objects.requireNonNull(routeId, "routeId không được trống.");
        Objects.requireNonNull(routeStatus, "routeStatus không được trống.");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
        Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
        Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
        Objects.requireNonNull(originLatitude, "originLatitude không được trống.");
        Objects.requireNonNull(originLongitude, "originLongitude không được trống.");
        Objects.requireNonNull(originAddress, "originAddress không được trống.");
        Objects.requireNonNull(driverDestinationLatitude, "driverDestinationLatitude không được trống.");
        Objects.requireNonNull(driverDestinationLongitude, "driverDestinationLongitude không được trống.");
        Objects.requireNonNull(driverDestinationAddress, "driverDestinationAddress không được trống.");
        Objects.requireNonNull(originalRouteGeoJson, "originalRouteGeoJson không được trống.");
        Objects.requireNonNull(originalDistanceMeters, "originalDistanceMeters không được trống.");
        Objects.requireNonNull(originalDurationSeconds, "originalDurationSeconds không được trống.");
    }
}
