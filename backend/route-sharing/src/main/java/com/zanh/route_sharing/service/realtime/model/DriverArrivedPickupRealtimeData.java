package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record DriverArrivedPickupRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        String pickupStatus,
        Instant arrivedAt,
        Instant waitingDeadline) {
}
