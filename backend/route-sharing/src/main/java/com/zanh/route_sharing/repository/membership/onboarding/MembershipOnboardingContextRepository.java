package com.zanh.route_sharing.repository.membership.onboarding;

import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.repository.membership.onboarding.model.MembershipOnboardingContext;

public interface MembershipOnboardingContextRepository {
    MembershipOnboardingContext requireReadContext(Long actorId);

    MembershipOnboardingContext lockContext(Long actorId);

    NhaTruong lockSchool(Long schoolId);
}
