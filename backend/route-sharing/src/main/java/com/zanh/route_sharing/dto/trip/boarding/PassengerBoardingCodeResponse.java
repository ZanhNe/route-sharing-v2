package com.zanh.route_sharing.dto.trip.boarding;

import java.util.Objects;

public record PassengerBoardingCodeResponse(
        Long tripId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        String boardingCode) {
    public PassengerBoardingCodeResponse {
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(pickupStopId);
        Objects.requireNonNull(pickupStopOrder);
        if (boardingCode == null || !boardingCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("boardingCode phải gồm đúng 6 chữ số.");
        }
    }
}
