package com.zanh.route_sharing.dto.complaint.review;

import java.time.Instant;

public record ComplaintReviewActionResponse(Long complaintId, String status, Long currentReviewerId,
                Instant reviewStartedAt, Instant responseDeadline, Instant evidenceRequestDeadline,
                Instant resolvedAt, boolean changed) {
}
