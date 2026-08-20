package com.zanh.route_sharing.service.complaint.evidence.model;

import com.zanh.route_sharing.repository.complaint.evidence.model.EvidenceMetadataRow;

public record EvidenceUploadResult(EvidenceMetadataRow evidence, boolean created) {
}
