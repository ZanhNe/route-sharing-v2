package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.complaint.evidence.EvidenceUploadResponse;
import com.zanh.route_sharing.dto.complaint.review.ComplaintReviewerEvidencePageResponse;
import com.zanh.route_sharing.service.complaint.evidence.model.EvidenceDownloadResult;
import com.zanh.route_sharing.service.complaint.evidence.model.EvidencePageResult;
import org.springframework.web.multipart.MultipartFile;

public interface ComplaintReviewEvidenceService {
    EvidenceUploadResponse upload(Long actorId, Long complaintId, MultipartFile file, String description);

    EvidencePageResult listOwn(Long actorId, Long complaintId, int page, int size);

    EvidenceDownloadResult downloadOwn(Long actorId, Long complaintId, Long evidenceId);

    ComplaintReviewerEvidencePageResponse listReviewer(Long actorId, Long complaintId, int page, int size);

    EvidenceDownloadResult downloadReviewer(Long actorId, Long complaintId, Long evidenceId, String ip,
            String userAgent);
}
