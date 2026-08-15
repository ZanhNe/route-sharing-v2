package com.zanh.route_sharing.dto.trip.safety;

import java.time.Instant;

public record SafetyIncidentHandlingResponse(
        Long incidentId,
        Long tripId,
        String status,
        Handler primaryHandler,
        Instant acknowledgedAt,
        Instant resolvedAt,
        String safeConclusion,
        Instant changedAt) {
    public record Handler(Long userId, String fullName) {}
}
