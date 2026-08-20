package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.complaint.evidence.EvidenceUploadResponse;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import org.springframework.web.multipart.MultipartFile;

public interface ComplaintEvidenceService {
    EvidenceUploadResponse upload(Long actorId, Long complaintId, MultipartFile file, String description);

    EvidencePageResult listOwn(Long actorId, Long complaintId, int page, int size);

    EvidenceDownloadResult downloadOwn(Long actorId, Long complaintId, Long evidenceId);
}
