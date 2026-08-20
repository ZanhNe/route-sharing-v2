package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.complaint.review.*;

public interface ComplaintReviewService {
    ComplaintReviewQueueResponse queue(Long actorId, String status, int page, int size);

    ComplaintReviewCaseResponse reviewerCase(Long actorId, Long complaintId);

    ComplaintReviewActionResponse claim(Long actorId, Long complaintId);

    EligibleReviewerPageResponse eligibleReviewers(Long actorId, Long complaintId, int page, int size);

    ComplaintReviewActionResponse reassign(Long actorId, Long complaintId, ComplaintReassignRequest request);

    ComplaintParticipantReviewResponse participantView(Long actorId, Long complaintId);

    ComplaintFormalResponseResponse respond(Long actorId, Long complaintId, ComplaintResponseRequest request);

    ComplaintReviewActionResponse requestMoreEvidence(Long actorId, Long complaintId, ComplaintEvidenceRequest request);

    ComplaintReviewActionResponse resume(Long actorId, Long complaintId);

    ComplaintReviewActionResponse finalizeReview(Long actorId, Long complaintId,
            ComplaintReviewDecisionRequest request);

    ComplaintInvestigationContextResponse investigation(Long actorId, Long complaintId, String ip, String userAgent);

    ComplaintLocationEvidencePageResponse locations(Long actorId, Long complaintId, int page, int size, String ip,
            String userAgent);
}
