package com.zanh.route_sharing.service.evidence;

public record EvidenceInspection(String verifiedMediaType) {
    public EvidenceInspection {
        if (verifiedMediaType == null || verifiedMediaType.isBlank()) {
            throw new IllegalArgumentException("Verified media type không hợp lệ.");
        }
    }
}
