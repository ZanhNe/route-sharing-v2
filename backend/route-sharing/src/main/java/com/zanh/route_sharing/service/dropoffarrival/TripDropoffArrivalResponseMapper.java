package com.zanh.route_sharing.service.dropoffarrival;

import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalResponse;
import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalStopResponse;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripDropoffArrivalResponseMapper {
    public TripDropoffArrivalResponse toResponse(TripDropoffArrivalCommitResult result) {
        return new TripDropoffArrivalResponse(
                result.tripId(), result.routeId(), result.tripStatus().name(), result.rideRequestId(),
                result.bookingStatus().name(), result.actualPassengerCount(),
                new TripDropoffArrivalStopResponse(result.dropoffStopId(), result.dropoffStopOrder(),
                        result.dropoffStatus().name(), result.arrivedAt()));
    }
}
