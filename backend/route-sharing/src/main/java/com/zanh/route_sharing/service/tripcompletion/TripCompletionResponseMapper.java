package com.zanh.route_sharing.service.tripcompletion;

import com.zanh.route_sharing.dto.trip.completion.TripCompletionResponse;
import com.zanh.route_sharing.dto.trip.completion.TripCompletionStopResponse;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripCompletionResponseMapper {
    public TripCompletionResponse toResponse(TripCompletionCommitResult result) {
        return new TripCompletionResponse(
                result.tripId(),
                result.routeId(),
                result.tripStatus().name(),
                result.endedAt(),
                result.actualPassengerCount(),
                new TripCompletionStopResponse(
                        result.driverEndStopId(),
                        result.driverEndStopOrder(),
                        result.driverEndStatus().name(),
                        result.driverEndCompletedAt()));
    }
}
