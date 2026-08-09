package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record BookingRejectedRealtimeData(
        Long rideRequestId,
        Long routeId,
        String status,
        Instant decisionAt,
        Instant cooldownUntil) {
}
