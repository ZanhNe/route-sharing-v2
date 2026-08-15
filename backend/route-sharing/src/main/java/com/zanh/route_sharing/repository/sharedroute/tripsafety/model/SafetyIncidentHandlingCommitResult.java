package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import java.time.Instant;
import java.util.List;

public record SafetyIncidentHandlingCommitResult(
        Long incidentId, Long tripId, String status,
        Long primaryHandlerUserId, String primaryHandlerFullName,
        Instant acknowledgedAt, Instant resolvedAt, String safeConclusion,
        Instant changedAt, String changeType, boolean changed,
        Long reporterUserId, List<Long> safetyRealtimeRecipientUserIds) {
    public SafetyIncidentHandlingCommitResult {
        safetyRealtimeRecipientUserIds = safetyRealtimeRecipientUserIds == null ? List.of() : List.copyOf(safetyRealtimeRecipientUserIds);
    }
}
