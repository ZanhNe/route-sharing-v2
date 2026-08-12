package com.zanh.route_sharing.dto.trip.boarding;

import java.time.Instant;
import java.util.Objects;

public record TripBoardingResponse(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        String bookingStatus,
        String pickupStatus,
        Instant boardedAt,
        Integer actualPassengerCount) {
    public TripBoardingResponse {
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(routeId);
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(pickupStopId);
        Objects.requireNonNull(pickupStopOrder);
        Objects.requireNonNull(bookingStatus);
        Objects.requireNonNull(pickupStatus);
        Objects.requireNonNull(boardedAt);
        Objects.requireNonNull(actualPassengerCount);
    }
}
