package com.zanh.route_sharing.service.riderequest.cancellation;

import com.zanh.route_sharing.dto.riderequest.cancellation.RideRequestCancellationResponse;
import com.zanh.route_sharing.service.riderequest.cancellation.model.RideRequestCancellationResult;
import org.springframework.stereotype.Component;

@Component
public class RideRequestCancellationResponseMapper {
    public RideRequestCancellationResponse toResponse(RideRequestCancellationResult result) {
        return new RideRequestCancellationResponse(
                result.routeId(), result.rideRequestId(), result.previousStatus(), result.status(),
                result.cancelledAt(), result.remainingSeats(), result.reason());
    }
}
