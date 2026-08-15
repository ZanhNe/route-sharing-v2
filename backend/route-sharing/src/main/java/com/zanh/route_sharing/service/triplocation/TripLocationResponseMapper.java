package com.zanh.route_sharing.service.triplocation;

import com.zanh.route_sharing.dto.trip.location.TripLocationResponse;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripLocationResponseMapper {

    public TripLocationResponse toResponse(TripLocationCommitResult result) {
        if (result == null || result.outcome() == null) {
            throw new IllegalArgumentException("TripLocationCommitResult không hợp lệ.");
        }
        return new TripLocationResponse(
                result.tripId(),
                result.locationRecordId(),
                result.outcome().name(),
                result.observedAt(),
                result.receivedAt(),
                result.currentLocationUpdated(),
                result.recommendedSubmissionIntervalSeconds());
    }
}
