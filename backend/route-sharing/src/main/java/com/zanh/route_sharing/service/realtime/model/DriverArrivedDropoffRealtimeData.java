package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record DriverArrivedDropoffRealtimeData(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long dropoffStopId,
        Integer dropoffStopOrder,
        String dropoffStatus,
        Instant arrivedAt) {
}
