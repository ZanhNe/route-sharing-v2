package com.zanh.route_sharing.repository.complaint.evidence;

import com.zanh.route_sharing.domain.entity.KhieuNai;
import com.zanh.route_sharing.repository.complaint.evidence.model.*;
import java.util.Optional;

public interface ComplaintEvidenceRepository {
    KhieuNai lockOwnedComplaint(Long actorId, Long complaintId);

    Optional<EvidenceMetadataRow> findExisting(Long complaintId, Long uploaderId, String sha256Hex);

    EvidenceMetadataRow persist(EvidenceCommitCommand command, KhieuNai complaint);

    EvidencePageSnapshot findOwnPage(Long actorId, Long complaintId, int page, int size);

    Optional<EvidenceMetadataRow> findOwnEvidence(Long actorId, Long complaintId, Long evidenceId);
}
