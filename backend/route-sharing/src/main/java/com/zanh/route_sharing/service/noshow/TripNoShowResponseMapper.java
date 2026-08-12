package com.zanh.route_sharing.service.noshow;

import com.zanh.route_sharing.dto.trip.noshow.TripNoShowResponse;
import com.zanh.route_sharing.repository.sharedroute.noshow.model.TripNoShowCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripNoShowResponseMapper {
    public TripNoShowResponse toResponse(TripNoShowCommitResult result) {
        return new TripNoShowResponse(
                result.tripId(), result.routeId(), result.rideRequestId(),
                result.pickupStopId(), result.pickupStopOrder(),
                result.dropoffStopId(), result.dropoffStopOrder(),
                result.bookingStatus().name(), result.pickupStatus().name(), result.dropoffStatus().name(),
                result.noShowAt());
    }
}
