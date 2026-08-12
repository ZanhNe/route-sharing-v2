package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record PassengerBoardedRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        String bookingStatus,
        String pickupStatus,
        Instant boardedAt) {
}
