package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripParticipantSafetyChangedRealtimeData(
        Long tripId,
        Long interventionId,
        Long rideRequestId,
        String changeType,
        String bookingStatus,
        Instant changedAt) {
}
