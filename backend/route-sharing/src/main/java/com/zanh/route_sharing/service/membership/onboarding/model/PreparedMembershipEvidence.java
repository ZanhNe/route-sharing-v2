package com.zanh.route_sharing.service.membership.onboarding.model;

import com.zanh.route_sharing.domain.enums.ViTriBangChungThanhVien;
import com.zanh.route_sharing.storage.evidence.StagedBinary;

public record PreparedMembershipEvidence(ViTriBangChungThanhVien slot, StagedBinary staged,
        String originalFilename, String verifiedMediaType) {}
