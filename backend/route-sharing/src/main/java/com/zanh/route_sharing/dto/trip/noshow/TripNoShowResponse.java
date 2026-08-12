package com.zanh.route_sharing.dto.trip.noshow;

import java.time.Instant;
import java.util.Objects;

public record TripNoShowResponse(
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
    public TripNoShowResponse {
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(routeId);
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(pickupStopId);
        Objects.requireNonNull(pickupStopOrder);
        Objects.requireNonNull(dropoffStopId);
        Objects.requireNonNull(dropoffStopOrder);
        Objects.requireNonNull(bookingStatus);
        Objects.requireNonNull(pickupStatus);
        Objects.requireNonNull(dropoffStatus);
        Objects.requireNonNull(noShowAt);
    }
}
