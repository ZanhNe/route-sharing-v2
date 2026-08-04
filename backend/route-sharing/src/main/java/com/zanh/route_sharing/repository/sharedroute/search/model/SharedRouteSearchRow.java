package com.zanh.route_sharing.repository.sharedroute.search.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;

import java.math.BigDecimal;
import java.time.Instant;

public record SharedRouteSearchRow(
        Long sharedRouteId,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,

        Long driverId,
        String driverName,
        String driverAvatarUrl,

        Long vehicleId,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName,

        BigDecimal originLatitude,
        BigDecimal originLongitude,
        String originAddress,

        BigDecimal driverDestinationLatitude,
        BigDecimal driverDestinationLongitude,
        String driverDestinationAddress,

        BigDecimal pickupProjectionLatitude,
        BigDecimal pickupProjectionLongitude,

        BigDecimal proposedDropoffLatitude,
        BigDecimal proposedDropoffLongitude,

        String routeGeoJson,
        Instant expectedDepartureTime,
        Integer remainingSeats,

        BigDecimal suggestedSupportPerKm,
        BigDecimal pickupDeviationMeters,
        BigDecimal destinationDeviationMeters,
        BigDecimal sharedSegmentMeters) {
}
