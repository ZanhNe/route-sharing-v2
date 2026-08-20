package com.zanh.route_sharing.service.membership.onboarding.model;

import com.zanh.route_sharing.dto.membership.onboarding.MembershipProfileResponse;

public record MembershipDraftCommitResult(MembershipProfileResponse response, boolean created) {}
