package com.zanh.route_sharing.service.boarding;

import com.zanh.route_sharing.dto.trip.boarding.PassengerBoardingCodeResponse;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeResult;
import org.springframework.stereotype.Component;

@Component
public class PassengerBoardingCodeResponseMapper {
    public PassengerBoardingCodeResponse toResponse(PassengerBoardingCodeResult result) {
        return new PassengerBoardingCodeResponse(
                result.tripId(), result.rideRequestId(), result.pickupStopId(), result.pickupStopOrder(), result.boardingCode());
    }
}
