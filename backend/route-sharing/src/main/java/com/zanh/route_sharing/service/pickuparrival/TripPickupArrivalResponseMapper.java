package com.zanh.route_sharing.service.pickuparrival;

import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalResponse;
import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalStopResponse;
import com.zanh.route_sharing.repository.sharedroute.pickuparrival.model.TripPickupArrivalCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripPickupArrivalResponseMapper {

    public TripPickupArrivalResponse toResponse(TripPickupArrivalCommitResult result) {
        return new TripPickupArrivalResponse(
                result.tripId(),
                result.routeId(),
                result.tripStatus().name(),
                result.rideRequestId(),
                result.bookingStatus().name(),
                result.actualPassengerCount(),
                new TripPickupArrivalStopResponse(
                        result.pickupStopId(),
                        result.pickupStopOrder(),
                        result.pickupStatus().name(),
                        result.arrivedAt(),
                        result.waitingStartedAt(),
                        result.waitingDeadline()));
    }
}
