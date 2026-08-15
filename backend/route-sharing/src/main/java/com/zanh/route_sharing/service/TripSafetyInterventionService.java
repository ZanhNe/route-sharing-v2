package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.safety.TripSafetyInterventionResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetySafeExitRequest;

public interface TripSafetyInterventionService {
    TripSafetyInterventionResponse confirmSafeExit(Long actorId, Long tripId, Long interventionId,
            TripSafetySafeExitRequest request);

    TripSafetyInterventionResponse abortTripFromHold(Long actorId, Long tripId, Long interventionId);

    TripSafetyInterventionResponse abortTripBySafety(Long actorId, Long incidentId);
}
