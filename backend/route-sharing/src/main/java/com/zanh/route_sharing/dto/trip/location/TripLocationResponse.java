package com.zanh.route_sharing.dto.trip.location;

import java.time.Instant;

public record TripLocationResponse(
        Long tripId,
        Long locationRecordId,
        String outcome,
        Instant observedAt,
        Instant receivedAt,
        boolean currentLocationUpdated,
        Long recommendedSubmissionIntervalSeconds) {
}
