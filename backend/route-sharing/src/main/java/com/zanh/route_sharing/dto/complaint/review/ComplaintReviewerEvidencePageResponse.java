package com.zanh.route_sharing.dto.complaint.review;

import com.zanh.route_sharing.dto.response.PageMeta;
import java.time.Instant;
import java.util.List;

public record ComplaintReviewerEvidencePageResponse(List<Item> items, PageMeta page) {
    public record Item(Long evidenceId, String uploaderRole, String category, String originalFilename,
            String verifiedMediaType, long sizeBytes, String description, Instant uploadedAt) {
    }
}
