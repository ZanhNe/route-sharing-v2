package com.zanh.route_sharing.dto.complaint.review;

import java.time.Instant;
import java.util.List;

public record ComplaintReviewCaseResponse(Long complaintId, String status, String title, String allegation,
        Instant submittedAt, Long tripId, Long rideRequestId, Long complainantId, Long respondentId,
        Long currentReviewerId, Instant reviewStartedAt, Long appliedResponseWindowHours,
        Instant responseDeadline, Long requestedParticipantId, String requestReason,
        Instant evidenceRequestAt, Instant evidenceRequestDeadline, Long evidenceIdWatermark,
        String finalConclusion, Instant resolvedAt, String respondentResponse, Instant respondentResponseAt,
        long complainantEvidenceCount, long respondentEvidenceCount, List<HistoryItem> history) {
    public record HistoryItem(long sequence, String action, String previousState, String resultingState,
            Long previousReviewerId, Long resultingReviewerId, Long actorId,
            Long targetParticipantId, Instant occurredAt, Instant deadlineAt,
            Long evidenceIdWatermark, String reason) {
    }
}
