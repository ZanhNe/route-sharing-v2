package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record RouteCancelledByDriverRealtimeData(
        Long routeId,
        String routeStatus,
        Long rideRequestId,
        String rideRequestStatus,
        Instant cancelledAt) {
}
