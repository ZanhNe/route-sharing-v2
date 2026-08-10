package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripStartedRealtimeData(
                Long tripId,
                Long routeId,
                Long rideRequestId,
                String tripStatus,
                Instant startedAt) {
}
