package com.zanh.route_sharing.repository.sharedroute.tripsafety;

import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentSummarySnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommand;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommitResult;

import java.time.LocalDate;

public interface TripSafetyIncidentRepository {
    TripSafetyIncidentCommitResult commit(TripSafetyIncidentCommand command);

    SafetyIncidentSummarySnapshot findAuthorizedSummary(Long actorId, Long incidentId, LocalDate businessDate);
}
