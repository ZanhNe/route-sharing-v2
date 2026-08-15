package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TripSafetyInterventionSnapshot(
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
        BigDecimal safeExitLatitude,
        BigDecimal safeExitLongitude,
        Instant changedAt,
        String changeType) {
}
