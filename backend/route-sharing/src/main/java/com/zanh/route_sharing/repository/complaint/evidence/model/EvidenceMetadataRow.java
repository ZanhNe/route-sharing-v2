package com.zanh.route_sharing.repository.complaint.evidence.model;

import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import java.time.Instant;

public record EvidenceMetadataRow(Long evidenceId, Long complaintId, Long uploaderId,
        LoaiTepMinhChung category, String originalFilename, String verifiedMediaType,
        long sizeBytes, String fingerprint, String storageKey, Instant uploadedAt, String description) {}
