package com.zanh.route_sharing.repository.sharedroute.triplocation.model;

import java.time.Instant;

public record TripLocationCommitResult(
        Long tripId,
        Long locationRecordId,
        TripLocationCommitOutcome outcome,
        Instant observedAt,
        Instant receivedAt,
        boolean currentLocationUpdated,
        Long recommendedSubmissionIntervalSeconds,
        TripCurrentLocationFact currentLocationFact) {
}
