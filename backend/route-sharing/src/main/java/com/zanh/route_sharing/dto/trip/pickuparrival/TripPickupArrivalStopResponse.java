package com.zanh.route_sharing.dto.trip.pickuparrival;

import java.time.Instant;

public record TripPickupArrivalStopResponse(
        Long stopId,
        Integer order,
        String status,
        Instant arrivedAt,
        Instant waitingStartedAt,
        Instant waitingDeadline) {
}
