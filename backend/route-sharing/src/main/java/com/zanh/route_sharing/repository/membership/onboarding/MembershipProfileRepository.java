package com.zanh.route_sharing.repository.membership.onboarding;

import com.zanh.route_sharing.domain.entity.HoSoThanhVien;
import com.zanh.route_sharing.domain.entity.HoSoSinhVien;

import java.util.Optional;

public interface MembershipProfileRepository {
    Optional<HoSoThanhVien> findCurrent(Long actorId, Long schoolId);

    Optional<HoSoThanhVien> lockCurrent(Long actorId, Long schoolId);

    HoSoSinhVien persistNew(HoSoSinhVien profile);

    void forceVersionIncrement(HoSoThanhVien profile);

    void flush();

    boolean studentCodeReservedByOtherIdentity(Long schoolId, String normalizedStudentCode, Long currentProfileId);
}
