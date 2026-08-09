package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripFormedRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        String routeStatus,
        String tripStatus,
        Instant formedAt,
        Instant expectedDepartureTime) {
}
