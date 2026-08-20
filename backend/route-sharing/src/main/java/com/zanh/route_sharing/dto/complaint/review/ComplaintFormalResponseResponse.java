package com.zanh.route_sharing.dto.complaint.review;

import java.time.Instant;

public record ComplaintFormalResponseResponse(Long complaintId, Long responseId, String content,
                Instant submittedAt, boolean created) {
}
