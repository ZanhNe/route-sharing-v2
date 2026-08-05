package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPersistedView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RideRequestResponseMapper {

    public RideRequestResponse toResponse(RideRequestPersistedView view) {
        return new RideRequestResponse(
                view.rideRequestId(),
                view.routeId(),
                view.status(),
                view.sentAt(),
                view.expiresAt(),
                false,
                view.matchType(),
                view.dropoffType(),
                point(view.pickup()),
                point(view.passengerDestination()),
                point(view.proposedDropoff()),
                view.pickupDeviationMeters(),
                view.pickupDeviationSeconds(),
                view.passengerDesiredDistanceMeters(),
                view.servedDistanceMeters(),
                view.remainingDistanceMeters(),
                view.convenienceRatioPercent(),
                view.suggestedSupportPerKm(),
                view.proposedSupportAmount(),
                view.agreedSupportAmount());
    }

    private static RideRequestPointResponse point(RideRequestPointSnapshot source) {
        return new RideRequestPointResponse(
                BigDecimal.valueOf(source.point().getY()),
                BigDecimal.valueOf(source.point().getX()),
                source.address());
    }
}
