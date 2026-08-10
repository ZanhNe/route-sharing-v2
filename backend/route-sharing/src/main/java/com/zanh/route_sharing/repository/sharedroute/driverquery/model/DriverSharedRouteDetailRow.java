package com.zanh.route_sharing.repository.sharedroute.driverquery.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record DriverSharedRouteDetailRow(
        Long routeId,
        TrangThaiLoTrinh status,
        Instant createdAt,
        Instant expectedDepartureTime,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        String originAddress,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        String destinationAddress,
        Integer offeredSeats,
        Integer remainingSeats,
        BigDecimal suggestedSupportPerKm,
        String originalRouteGeoJson,
        BigDecimal originalDistanceMeters,
        Long originalDurationSeconds,
        Long vehicleId,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName,
        long totalRequests,
        long pendingRequests,
        long acceptedBookings,
        long rejectedRequests,
        long cancelledByPassenger,
        long cancelledByDriver,
        boolean assignedToTrip,
        Long tripId,
        Instant cancelledAt,
        String cancellationReason) {

    public DriverSharedRouteDetailRow {
        Objects.requireNonNull(routeId, "routeId không được trống.");
        Objects.requireNonNull(status, "status không được trống.");
        Objects.requireNonNull(createdAt, "createdAt không được trống.");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
        Objects.requireNonNull(originLatitude, "originLatitude không được trống.");
        Objects.requireNonNull(originLongitude, "originLongitude không được trống.");
        Objects.requireNonNull(originAddress, "originAddress không được trống.");
        Objects.requireNonNull(destinationLatitude, "destinationLatitude không được trống.");
        Objects.requireNonNull(destinationLongitude, "destinationLongitude không được trống.");
        Objects.requireNonNull(destinationAddress, "destinationAddress không được trống.");
        Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
        Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
        Objects.requireNonNull(originalRouteGeoJson, "originalRouteGeoJson không được trống.");
        Objects.requireNonNull(originalDistanceMeters, "originalDistanceMeters không được trống.");
        Objects.requireNonNull(originalDurationSeconds, "originalDurationSeconds không được trống.");
        Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
    }

    public DriverSharedRouteDetailRow(
            Long routeId, TrangThaiLoTrinh status, Instant createdAt, Instant expectedDepartureTime,
            BigDecimal originLatitude, BigDecimal originLongitude, String originAddress,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude, String destinationAddress,
            Integer offeredSeats, Integer remainingSeats, BigDecimal suggestedSupportPerKm,
            String originalRouteGeoJson, BigDecimal originalDistanceMeters, Long originalDurationSeconds,
            Long vehicleId, String licensePlate, String actualColor, String brandName, String modelName,
            long totalRequests, long pendingRequests, long acceptedBookings, long rejectedRequests,
            long cancelledByPassenger, long cancelledByDriver, boolean assignedToTrip,
            Instant cancelledAt, String cancellationReason) {
        this(routeId, status, createdAt, expectedDepartureTime, originLatitude, originLongitude, originAddress,
                destinationLatitude, destinationLongitude, destinationAddress, offeredSeats, remainingSeats,
                suggestedSupportPerKm, originalRouteGeoJson, originalDistanceMeters, originalDurationSeconds,
                vehicleId, licensePlate, actualColor, brandName, modelName, totalRequests, pendingRequests,
                acceptedBookings, rejectedRequests, cancelledByPassenger, cancelledByDriver, assignedToTrip,
                null, cancelledAt, cancellationReason);
    }
}
