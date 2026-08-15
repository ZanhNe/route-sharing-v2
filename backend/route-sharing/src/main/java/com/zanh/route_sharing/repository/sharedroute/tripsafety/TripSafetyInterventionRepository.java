package com.zanh.route_sharing.repository.sharedroute.tripsafety;

import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionCommitResult;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;

public interface TripSafetyInterventionRepository {
    TripSafetyInterventionCommitResult ensureInitialContainment(Long actorId, Long incidentId, Instant occurredAt);
    TripSafetyInterventionCommitResult confirmSafeExit(Long actorId, Long tripId, Long interventionId, Point position, Instant occurredAt);
    TripSafetyInterventionCommitResult abortTripFromHold(Long actorId, Long tripId, Long interventionId, Instant occurredAt);
    TripSafetyInterventionCommitResult abortTripBySafety(Long actorId, Long incidentId, Instant occurredAt, LocalDate businessDate);
}
