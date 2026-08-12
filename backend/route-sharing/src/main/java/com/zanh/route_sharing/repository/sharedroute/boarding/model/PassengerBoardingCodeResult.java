package com.zanh.route_sharing.repository.sharedroute.boarding.model;

public record PassengerBoardingCodeResult(
        Long tripId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        String boardingCode) {
}
