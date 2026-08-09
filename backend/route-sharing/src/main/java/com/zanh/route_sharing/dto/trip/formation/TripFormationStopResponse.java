package com.zanh.route_sharing.dto.trip.formation;

import java.math.BigDecimal;

public record TripFormationStopResponse(
        Long stopId,
        Integer order,
        String type,
        String status,
        Long rideRequestId,
        TripFormationPointResponse point,
        BigDecimal arrivalRadiusMeters) {
}
