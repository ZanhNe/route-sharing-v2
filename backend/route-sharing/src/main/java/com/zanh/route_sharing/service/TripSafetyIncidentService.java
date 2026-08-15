package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.safety.ReporterSafetyIncidentStatusResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentRequest;
import com.zanh.route_sharing.service.tripsafety.TripSafetyIncidentOperationResult;

public interface TripSafetyIncidentService {
    TripSafetyIncidentOperationResult report(Long actorId, Long tripId, TripSafetyIncidentRequest request);

    ReporterSafetyIncidentStatusResponse getOwnIncidentStatus(Long actorId, Long tripId, Long incidentId);
}
