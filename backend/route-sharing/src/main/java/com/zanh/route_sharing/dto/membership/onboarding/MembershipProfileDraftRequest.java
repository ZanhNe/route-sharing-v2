package com.zanh.route_sharing.dto.membership.onboarding;

import com.zanh.route_sharing.domain.enums.ViTriBangChungThanhVien;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Set;

public record MembershipProfileDraftRequest(
        Long expectedVersion,
        String studentCode,
        Boolean currentlyStudying,
        LocalDate enrollmentDate,
        @Positive(message = "classId phải là số dương.") Long classId,
        Set<ViTriBangChungThanhVien> removeEvidenceSlots) {
    public MembershipProfileDraftRequest {
        removeEvidenceSlots = removeEvidenceSlots == null ? Set.of() : Set.copyOf(removeEvidenceSlots);
    }
}
