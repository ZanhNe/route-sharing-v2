package com.zanh.route_sharing.repository.membership.onboarding;

import com.zanh.route_sharing.domain.entity.Lop;

import java.util.Optional;

public interface MembershipAcademicContextRepository {
    Optional<Lop> findClassInSchool(Long classId, Long schoolId);
}
