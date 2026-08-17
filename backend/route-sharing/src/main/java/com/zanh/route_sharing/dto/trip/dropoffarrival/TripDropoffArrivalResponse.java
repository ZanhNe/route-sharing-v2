package com.zanh.route_sharing.dto.trip.dropoffarrival;

public record TripDropoffArrivalResponse(
                Long tripId,
                Long routeId,
                String tripStatus,
                Long rideRequestId,
                String bookingStatus,
                Integer actualPassengerCount,
                TripDropoffArrivalStopResponse dropoff) {
}
