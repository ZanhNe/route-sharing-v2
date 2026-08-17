package com.zanh.route_sharing.dto.trip.dropoffverification;

import java.util.Objects;

public record PassengerDropoffCodeResponse(
        Long tripId,
        Long rideRequestId,
        Long dropoffStopId,
        Integer dropoffStopOrder,
        String dropoffCode) {
    public PassengerDropoffCodeResponse {
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(dropoffStopId);
        Objects.requireNonNull(dropoffStopOrder);
        if (dropoffCode == null || !dropoffCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("dropoffCode phải gồm đúng 6 chữ số.");
        }
    }
}
