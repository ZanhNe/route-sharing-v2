package com.zanh.route_sharing.repository.complaint.evidence.model;
import java.util.List;
public record EvidencePageSnapshot(List<EvidenceMetadataRow> items, int page, int size, long totalElements) {
    public EvidencePageSnapshot { items = List.copyOf(items); }
}
