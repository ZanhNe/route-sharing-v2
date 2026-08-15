package com.zanh.route_sharing.dto.trip.safety;

import java.time.Instant;

public record ReporterSafetyIncidentStatusResponse(
        Long incidentId, Long tripId, String type, String severity, Instant reportedAt,
        String status, Instant acknowledgedAt, Instant resolvedAt, String safeConclusion,
        Intervention intervention) {
    public record Intervention(Long interventionId, String type, String status, String tripStatus,
                               String ownBookingStatus, Instant changedAt) {}
    public ReporterSafetyIncidentStatusResponse(Long incidentId, Long tripId, String type, String severity, Instant reportedAt,
                                                String status, Instant acknowledgedAt, Instant resolvedAt, String safeConclusion) {
        this(incidentId, tripId, type, severity, reportedAt, status, acknowledgedAt, resolvedAt, safeConclusion, null);
    }
}
