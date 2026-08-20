package com.zanh.route_sharing.dto.membership.onboarding;

import java.time.LocalDate;
import java.util.List;

public record MembershipProfileResponse(
        Long profileId,
        String profileKind,
        String status,
        Long version,
        MembershipSchoolResponse school,
        String studentCode,
        Boolean currentlyStudying,
        LocalDate enrollmentDate,
        MembershipClassResponse classSummary,
        List<MembershipEvidenceResponse> evidence,
        MembershipSubmissionSummaryResponse submission,
        String nextAction) {
    public MembershipProfileResponse {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
