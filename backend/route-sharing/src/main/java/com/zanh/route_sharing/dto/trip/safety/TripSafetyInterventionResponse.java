package com.zanh.route_sharing.dto.trip.safety;

import java.math.BigDecimal;
import java.time.Instant;

public record TripSafetyInterventionResponse(
        Long interventionId,
        Long incidentId,
        Long tripId,
        String type,
        String status,
        String tripStatus,
        Long targetRideRequestId,
        String targetBookingStatus,
        Integer actualPassengerCount,
        Instant safeExitAt,
        Position safeExitPosition,
        Instant changedAt) {
    public record Position(BigDecimal latitude, BigDecimal longitude) {}
}
