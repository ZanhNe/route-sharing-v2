package com.zanh.route_sharing.dto.membership.onboarding;

import java.time.Instant;
import java.util.List;

public record MembershipSubmissionResponse(
        Long profileId,
        String profileKind,
        String profileStatus,
        Long version,
        MembershipSchoolResponse school,
        String studentCode,
        Long submissionId,
        int submissionNumber,
        Instant submittedAt,
        List<MembershipEvidenceResponse> evidence,
        String nextAction,
        String accountState,
        boolean created) {
    public MembershipSubmissionResponse {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
