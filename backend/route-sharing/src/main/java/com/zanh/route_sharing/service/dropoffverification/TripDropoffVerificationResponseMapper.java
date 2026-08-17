package com.zanh.route_sharing.service.dropoffverification;

import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationResponse;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripDropoffVerificationResponseMapper {
    public TripDropoffVerificationResponse toResponse(TripDropoffVerificationCommitResult result) {
        return new TripDropoffVerificationResponse(result.tripId(), result.routeId(), result.rideRequestId(),
                result.dropoffStopId(), result.dropoffStopOrder(), result.bookingStatus().name(),
                result.dropoffStatus().name(), result.droppedOffAt(), result.actualPassengerCount());
    }
}
