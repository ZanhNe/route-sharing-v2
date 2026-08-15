package com.zanh.route_sharing.dto.trip.safety;

import java.time.Instant;
import java.util.List;

public record SafetyIncidentQueueResponse(List<Item> items, SafetyPageMeta page) {
    public record Item(Long incidentId, Long tripId, School school, String type, String severity, String status,
                       String reporterSource, Instant reportedAt, Handler primaryHandler,
                       Instant acknowledgedAt, Instant resolvedAt) {}
    public record School(Long schoolId, String schoolName) {}
    public record Handler(Long userId, String fullName) {}
}
