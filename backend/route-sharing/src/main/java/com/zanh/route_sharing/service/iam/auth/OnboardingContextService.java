package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.dto.auth.entry.OnboardingContextResponse;

public interface OnboardingContextService {
    OnboardingContextResponse getCurrent(Long accountId);
}
