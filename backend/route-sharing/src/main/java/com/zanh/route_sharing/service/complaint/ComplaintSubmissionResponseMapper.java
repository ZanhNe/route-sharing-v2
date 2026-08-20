package com.zanh.route_sharing.service.complaint;

import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintResponse;
import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionResult;
import org.springframework.stereotype.Component;

@Component
public class ComplaintSubmissionResponseMapper {
    public SubmitComplaintResponse toResponse(ComplaintSubmissionResult result) {
        return new SubmitComplaintResponse(
                result.complaintId(),
                result.tripId(),
                result.rideRequestId(),
                result.targetUserId(),
                result.status().name(),
                result.submittedAt(),
                result.filingDeadlineApplied(),
                result.incidentId());
    }
}
