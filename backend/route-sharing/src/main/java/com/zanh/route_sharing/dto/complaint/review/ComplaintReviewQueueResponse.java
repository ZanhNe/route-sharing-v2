package com.zanh.route_sharing.dto.complaint.review;

import com.zanh.route_sharing.dto.response.PageMeta;
import java.time.Instant;
import java.util.List;

public record ComplaintReviewQueueResponse(List<Item> items, PageMeta page) {
    public record Item(Long complaintId, String status, Instant submittedAt, Long tripId, Long rideRequestId,
            String complainantRole, String respondentRole, Long currentReviewerId,
            Instant reviewStartedAt, Instant responseDeadline, Instant evidenceRequestDeadline,
            Instant resolvedAt) {
    }
}
