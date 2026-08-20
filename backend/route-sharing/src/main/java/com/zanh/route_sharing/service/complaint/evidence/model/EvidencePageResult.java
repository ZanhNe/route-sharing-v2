package com.zanh.route_sharing.service.complaint.evidence.model;

import com.zanh.route_sharing.dto.complaint.evidence.EvidencePageResponse;
import com.zanh.route_sharing.dto.response.PageMeta;

public record EvidencePageResult(EvidencePageResponse data, PageMeta meta) {
}
