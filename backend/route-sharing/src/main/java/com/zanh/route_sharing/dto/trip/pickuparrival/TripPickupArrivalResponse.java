package com.zanh.route_sharing.dto.trip.pickuparrival;

public record TripPickupArrivalResponse(
        Long tripId,
        Long routeId,
        String tripStatus,
        Long rideRequestId,
        String bookingStatus,
        Integer actualPassengerCount,
        TripPickupArrivalStopResponse pickup) {
}
