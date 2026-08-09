package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record BookingRequestRealtimeData(
        Long rideRequestId,
        Long routeId,
        String status,
        Instant sentAt) {
}
