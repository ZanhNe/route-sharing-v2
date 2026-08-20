package com.zanh.route_sharing.service.complaint.evidence;

import com.zanh.route_sharing.dto.complaint.evidence.*;
import com.zanh.route_sharing.repository.complaint.evidence.model.*;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import com.zanh.route_sharing.dto.response.PageMeta;
import org.springframework.stereotype.Component;

@Component
public class ComplaintEvidenceResponseMapper {
    public EvidenceUploadResponse upload(EvidenceUploadResult result) {
        EvidenceMetadataRow row = result.evidence();
        return new EvidenceUploadResponse(row.evidenceId(), row.complaintId(), row.category().name(),
                row.originalFilename(),
                row.verifiedMediaType(), row.sizeBytes(), row.fingerprint(), row.uploadedAt(), row.description(),
                result.created());
    }

    public EvidencePageResult page(EvidencePageSnapshot snapshot) {
        var items = snapshot.items().stream().map(this::item).toList();
        return new EvidencePageResult(new EvidencePageResponse(items),
                PageMeta.of(snapshot.page(), snapshot.size(), snapshot.totalElements()));
    }

    public EvidenceItemResponse item(EvidenceMetadataRow row) {
        return new EvidenceItemResponse(row.evidenceId(), row.complaintId(), row.category().name(),
                row.originalFilename(),
                row.verifiedMediaType(), row.sizeBytes(), row.fingerprint(), row.uploadedAt(), row.description());
    }
}
