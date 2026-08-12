package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record PassengerNoShowRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        Long dropoffStopId,
        Integer dropoffStopOrder,
        String bookingStatus,
        String pickupStatus,
        String dropoffStatus,
        Instant noShowAt) {
}
