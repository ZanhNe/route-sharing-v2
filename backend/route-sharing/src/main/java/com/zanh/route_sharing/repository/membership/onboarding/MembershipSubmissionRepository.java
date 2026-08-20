package com.zanh.route_sharing.repository.membership.onboarding;

import com.zanh.route_sharing.domain.entity.LanNopHoSoThanhVien;

import java.util.Optional;

public interface MembershipSubmissionRepository {
    Optional<LanNopHoSoThanhVien> findInitial(Long profileId);

    LanNopHoSoThanhVien persist(LanNopHoSoThanhVien submission);
}
