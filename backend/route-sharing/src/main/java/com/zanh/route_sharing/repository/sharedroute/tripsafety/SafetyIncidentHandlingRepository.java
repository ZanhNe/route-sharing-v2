package com.zanh.route_sharing.repository.sharedroute.tripsafety;

import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentHandlingCommitResult;
import java.time.Instant;
import java.time.LocalDate;

public interface SafetyIncidentHandlingRepository {
    SafetyIncidentHandlingCommitResult claim(Long actorId, Long incidentId, Instant occurredAt, LocalDate businessDate);
    SafetyIncidentHandlingCommitResult investigate(Long actorId, Long incidentId, Instant occurredAt, LocalDate businessDate);
    SafetyIncidentHandlingCommitResult reassign(Long actorId, Long incidentId, Long newHandlerUserId, String reason,
                                                 Instant occurredAt, LocalDate businessDate);
    SafetyIncidentHandlingCommitResult finalizeIncident(Long actorId, Long incidentId, TrangThaiXuLySuCo outcome,
                                                         String safeConclusion, Instant occurredAt, LocalDate businessDate);
}
