package com.zanh.route_sharing.service.boarding;

import com.zanh.route_sharing.dto.trip.boarding.TripBoardingResponse;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripBoardingResponseMapper {
    public TripBoardingResponse toResponse(TripBoardingCommitResult result) {
        return new TripBoardingResponse(
                result.tripId(), result.routeId(), result.rideRequestId(), result.pickupStopId(),
                result.pickupStopOrder(),
                result.bookingStatus().name(), result.pickupStatus().name(), result.boardedAt(),
                result.actualPassengerCount());
    }
}
