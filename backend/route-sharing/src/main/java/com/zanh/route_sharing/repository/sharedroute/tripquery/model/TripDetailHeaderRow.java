package com.zanh.route_sharing.repository.sharedroute.tripquery.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TripDetailHeaderRow(
        TripViewerRole viewerRole,
        Long tripId,
        TrangThaiVanHanhChuyenDi tripStatus,
        Instant formedAt,
        Instant startedAt,
        Instant cancelledAt,
        String cancellationReason,
        Integer plannedPassengerCount,
        Integer actualPassengerCount,
        TrangThaiDiemDung driverStartStatus,
        Instant driverStartCompletedAt,
        String operationalRouteGeoJson,
        Long routeId,
        TrangThaiLoTrinh routeStatus,
        Instant lockedAt,
        Instant expectedDepartureTime,
        Integer offeredSeats,
        Integer remainingSeats,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        String originAddress,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        String destinationAddress,
        Long driverId,
        String driverFullName,
        String driverAvatarUrl,
        Long vehicleId,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName) {
    public TripDetailHeaderRow {
        Objects.requireNonNull(viewerRole);
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(tripStatus);
        Objects.requireNonNull(formedAt);
        Objects.requireNonNull(plannedPassengerCount);
        Objects.requireNonNull(actualPassengerCount);
        Objects.requireNonNull(operationalRouteGeoJson);
        Objects.requireNonNull(routeId);
        Objects.requireNonNull(routeStatus);
        Objects.requireNonNull(lockedAt);
        Objects.requireNonNull(expectedDepartureTime);
        Objects.requireNonNull(offeredSeats);
        Objects.requireNonNull(remainingSeats);
        Objects.requireNonNull(originLatitude);
        Objects.requireNonNull(originLongitude);
        Objects.requireNonNull(originAddress);
        Objects.requireNonNull(destinationLatitude);
        Objects.requireNonNull(destinationLongitude);
        Objects.requireNonNull(destinationAddress);
        Objects.requireNonNull(driverId);
        Objects.requireNonNull(driverFullName);
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(licensePlate);
        Objects.requireNonNull(actualColor);
        Objects.requireNonNull(brandName);
        Objects.requireNonNull(modelName);
    }
}
