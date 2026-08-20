package com.zanh.route_sharing.dto.complaint.evidence;
import java.time.Instant;
public record EvidenceItemResponse(Long evidenceId, Long complaintId, String category, String originalFilename,
        String verifiedMediaType, long sizeBytes, String fingerprint, Instant uploadedAt, String description) {}
