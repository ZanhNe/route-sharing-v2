package com.zanh.route_sharing.dto.sharedroute.search;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;

import java.math.BigDecimal;
import java.time.Instant;

public record SharedRouteSearchItemResponse(
        Long sharedRouteId,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,

        SharedRouteDriverResponse driver,
        SharedRouteVehicleResponse vehicle,

        SearchPointResponse routeOrigin,
        SearchPointResponse driverDestination,
        SearchPointResponse pickupProjectionOnRoute,
        SearchPointResponse proposedDropoff,

        String routeGeoJson,
        Instant expectedDepartureTime,
        Integer remainingSeats,

        BigDecimal suggestedSupportPerKm,
        BigDecimal pickupDeviationMeters,
        BigDecimal destinationDeviationMeters,
        BigDecimal sharedSegmentMeters
) {
}
