package com.zanh.route_sharing.dto.trip.safety;

import java.time.Instant;

public record SafetyIncidentSummaryResponse(
        Long incidentId,
        Long tripId,
        String type,
        String severity,
        String status,
        String reporterSource,
        Instant reportedAt) {
}
