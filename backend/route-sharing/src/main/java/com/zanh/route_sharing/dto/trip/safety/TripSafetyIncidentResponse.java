package com.zanh.route_sharing.dto.trip.safety;

import java.time.Instant;

public record TripSafetyIncidentResponse(
        Long incidentId,
        Long tripId,
        String type,
        String severity,
        String status,
        Instant reportedAt,
        TripSafetyInterventionResponse intervention) {
    public TripSafetyIncidentResponse(Long incidentId, Long tripId, String type, String severity, String status, Instant reportedAt) {
        this(incidentId, tripId, type, severity, status, reportedAt, null);
    }
}
