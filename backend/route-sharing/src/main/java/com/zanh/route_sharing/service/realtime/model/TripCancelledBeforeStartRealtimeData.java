package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripCancelledBeforeStartRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        String tripStatus,
        String routeStatus,
        String rideRequestStatus,
        Instant cancelledAt) {
}
