package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record PassengerDroppedOffRealtimeData(
                Long tripId, Long routeId, Long rideRequestId, Long dropoffStopId, Integer dropoffStopOrder,
                String bookingStatus, String dropoffStatus, Instant droppedOffAt) {
}
