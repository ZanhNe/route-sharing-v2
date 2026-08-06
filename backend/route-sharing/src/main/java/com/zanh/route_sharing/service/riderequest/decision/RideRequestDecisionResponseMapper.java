package com.zanh.route_sharing.service.riderequest.decision;

import com.zanh.route_sharing.dto.riderequest.decision.RideRequestDecisionResponse;
import com.zanh.route_sharing.service.riderequest.decision.model.RideRequestDecisionResult;
import org.springframework.stereotype.Component;

@Component
public class RideRequestDecisionResponseMapper {

    public RideRequestDecisionResponse toResponse(RideRequestDecisionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result không được trống");
        }
        return new RideRequestDecisionResponse(
                result.routeId(),
                result.rideRequestId(),
                result.status(),
                result.decisionAt(),
                result.remainingSeats(),
                result.agreedSupportAmount(),
                result.cooldownUntil());
    }
}
