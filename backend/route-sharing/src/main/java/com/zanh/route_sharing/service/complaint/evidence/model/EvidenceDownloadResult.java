package com.zanh.route_sharing.service.complaint.evidence.model;

import org.springframework.core.io.Resource;

public record EvidenceDownloadResult(Resource resource, String originalFilename, String verifiedMediaType,
        long sizeBytes) {
}
