package com.zanh.route_sharing.service.dropoffverification;

import com.zanh.route_sharing.dto.trip.dropoffverification.PassengerDropoffCodeResponse;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeResult;
import org.springframework.stereotype.Component;

@Component
public class PassengerDropoffCodeResponseMapper {
    public PassengerDropoffCodeResponse toResponse(PassengerDropoffCodeResult result) {
        return new PassengerDropoffCodeResponse(result.tripId(), result.rideRequestId(), result.dropoffStopId(),
                result.dropoffStopOrder(), result.dropoffCode());
    }
}
