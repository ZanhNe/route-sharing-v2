package com.zanh.route_sharing.dto.trip.start;

import java.time.Instant;

public record TripStartResponse(
        Long tripId,
        Long routeId,
        String tripStatus,
        Instant startedAt,
        Integer actualPassengerCount,
        TripStartStopResponse driverStart) {
}
