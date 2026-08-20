package com.zanh.route_sharing.dto.complaint.review;

import java.time.Instant;

public record ComplaintParticipantReviewResponse(Long complaintId, String status, String title, String allegation,
                Instant submittedAt, Long tripId, Long rideRequestId, String actorRole,
                Instant reviewStartedAt, Instant responseDeadline, Long requestedParticipantId,
                String requestReason, Instant evidenceRequestDeadline, String conclusion, Instant resolvedAt,
                String ownResponse, Instant ownResponseAt, long ownEvidenceCount) {
}
