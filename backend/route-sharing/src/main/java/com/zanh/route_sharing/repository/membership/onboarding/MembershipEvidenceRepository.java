package com.zanh.route_sharing.repository.membership.onboarding;

import com.zanh.route_sharing.domain.entity.BangChungThanhVien;
import com.zanh.route_sharing.repository.membership.onboarding.model.MembershipEvidenceBinaryMetadata;

import java.util.List;
import java.util.Optional;

public interface MembershipEvidenceRepository {
    List<BangChungThanhVien> findCurrent(Long profileId);

    BangChungThanhVien persist(BangChungThanhVien evidence);

    void flush();

    Optional<MembershipEvidenceBinaryMetadata> findOwnedBinary(Long actorId, Long evidenceId);
}
