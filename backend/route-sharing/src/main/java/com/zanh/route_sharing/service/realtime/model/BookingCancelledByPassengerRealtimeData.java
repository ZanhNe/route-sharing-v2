package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record BookingCancelledByPassengerRealtimeData(
        Long rideRequestId,
        Long routeId,
        String previousStatus,
        String status,
        Instant cancelledAt,
        Integer remainingSeats) {
}
