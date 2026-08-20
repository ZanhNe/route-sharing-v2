package com.zanh.route_sharing.repository.membership.onboarding.model;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhaTruong;

public record MembershipOnboardingContext(NguoiDung account, NhaTruong school) {
    public MembershipOnboardingContext {
        if (account == null || school == null) throw new IllegalArgumentException("Membership onboarding context không hợp lệ.");
    }
}
