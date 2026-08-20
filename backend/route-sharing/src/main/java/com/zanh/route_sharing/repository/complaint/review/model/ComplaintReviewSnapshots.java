package com.zanh.route_sharing.repository.complaint.review.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ComplaintReviewSnapshots {
    private ComplaintReviewSnapshots() {}

    public record Page<T>(List<T> items, int page, int size, long totalElements) {}

    public record QueueItem(Long complaintId, String status, Instant submittedAt, Long tripId, Long rideRequestId,
                            String complainantRole, String respondentRole, Long currentReviewerId,
                            Instant reviewStartedAt, Instant responseDeadline, Instant evidenceRequestDeadline,
                            Instant resolvedAt) {}

    public record HistoryItem(long sequence, String action, String previousState, String resultingState,
                              Long previousReviewerId, Long resultingReviewerId, Long actorId,
                              Long targetParticipantId, Instant occurredAt, Instant deadlineAt,
                              Long evidenceIdWatermark, String reason) {}

    public record ReviewCase(Long complaintId, String status, String title, String allegation,
                             Instant submittedAt, Long tripId, Long rideRequestId,
                             Long complainantId, Long respondentId, Long currentReviewerId,
                             Instant reviewStartedAt, Long appliedResponseWindowHours,
                             Instant responseDeadline, Long requestedParticipantId, String requestReason,
                             Instant evidenceRequestAt, Instant evidenceRequestDeadline,
                             Long evidenceIdWatermark, String finalConclusion, Instant resolvedAt,
                             String respondentResponse, Instant respondentResponseAt,
                             long complainantEvidenceCount, long respondentEvidenceCount,
                             List<HistoryItem> history) {}

    public record ParticipantView(Long complaintId, String status, String title, String allegation,
                                  Instant submittedAt, Long tripId, Long rideRequestId,
                                  String actorRole, Instant reviewStartedAt, Instant responseDeadline,
                                  Long requestedParticipantId, String requestReason,
                                  Instant evidenceRequestDeadline, String conclusion, Instant resolvedAt,
                                  String ownResponse, Instant ownResponseAt, long ownEvidenceCount) {}

    public record Action(Long complaintId, String status, Long currentReviewerId,
                         Instant reviewStartedAt, Instant responseDeadline,
                         Instant evidenceRequestDeadline, Instant resolvedAt, boolean changed) {}

    public record FormalResponse(Long complaintId, Long responseId, String content,
                                 Instant submittedAt, boolean created) {}

    public record EligibleReviewer(Long reviewerId, String displayName) {}

    public record Evidence(Long evidenceId, String uploaderRole, Long uploaderId, String category,
                           String originalFilename, String verifiedMediaType, long sizeBytes,
                           String fingerprint, String storageKey, String description, Instant uploadedAt) {}

    public record Investigation(Long complaintId, Long tripId, String tripStatus,
                                Long rideRequestId, String rideRequestStatus,
                                Long driverId, Long passengerId,
                                long stopCount, Long linkedIncidentId, String linkedIncidentType,
                                String linkedIncidentStatus, Instant tripEndedAt) {}

    public record Location(Long locationId, long sequence, Instant observedAt, Instant receivedAt,
                           BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters) {}
}
