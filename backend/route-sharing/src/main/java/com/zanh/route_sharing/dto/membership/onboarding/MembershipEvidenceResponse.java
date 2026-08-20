package com.zanh.route_sharing.dto.membership.onboarding;

import com.zanh.route_sharing.domain.enums.ViTriBangChungThanhVien;

import java.time.Instant;

public record MembershipEvidenceResponse(Long evidenceId, ViTriBangChungThanhVien slot,
                String originalFilename, String verifiedMediaType, long sizeBytes, Instant uploadedAt) {
}
