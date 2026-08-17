package com.zanh.route_sharing.dto.trip.dropoffverification;

import java.time.Instant;
import java.util.Objects;

public record TripDropoffVerificationResponse(
        Long tripId, Long routeId, Long rideRequestId, Long dropoffStopId, Integer dropoffStopOrder,
        String bookingStatus, String dropoffStatus, Instant droppedOffAt, Integer actualPassengerCount) {
    public TripDropoffVerificationResponse {
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(routeId);
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(dropoffStopId);
        Objects.requireNonNull(dropoffStopOrder);
        Objects.requireNonNull(bookingStatus);
        Objects.requireNonNull(dropoffStatus);
        Objects.requireNonNull(droppedOffAt);
        Objects.requireNonNull(actualPassengerCount);
    }
}
