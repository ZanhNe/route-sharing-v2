package com.zanh.route_sharing.dto.complaint.submission;

import java.time.Instant;

public record SubmitComplaintResponse(
                Long complaintId,
                Long tripId,
                Long rideRequestId,
                Long targetUserId,
                String complaintStatus,
                Instant submittedAt,
                Instant filingDeadlineApplied,
                Long incidentId) {
}
