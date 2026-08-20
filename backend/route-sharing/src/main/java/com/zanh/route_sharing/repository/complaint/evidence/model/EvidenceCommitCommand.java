package com.zanh.route_sharing.repository.complaint.evidence.model;

import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import java.time.Instant;

public record EvidenceCommitCommand(Long actorId, Long complaintId, LoaiTepMinhChung category,
        String originalFilename, String verifiedMediaType, long sizeBytes, String sha256Hex,
        String storageKey, String description, Instant uploadedAt) {}
