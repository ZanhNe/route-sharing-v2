package com.zanh.route_sharing.dto.membership.onboarding;

import java.time.Instant;

public record MembershipSubmissionSummaryResponse(Long submissionId, int submissionNumber, Instant submittedAt) {
}
