package com.zanh.route_sharing.service.tripstart;

import com.zanh.route_sharing.dto.trip.start.TripStartResponse;
import com.zanh.route_sharing.dto.trip.start.TripStartStopResponse;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripStartResponseMapper {

    public TripStartResponse toResponse(TripStartCommitResult result) {
        return new TripStartResponse(
                result.tripId(),
                result.routeId(),
                result.tripStatus().name(),
                result.startedAt(),
                result.actualPassengerCount(),
                new TripStartStopResponse(
                        result.driverStartStopId(),
                        result.driverStartStatus().name()));
    }
}
