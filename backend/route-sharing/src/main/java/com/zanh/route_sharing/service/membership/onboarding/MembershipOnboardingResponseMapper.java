package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.TrangThaiHocTap;
import com.zanh.route_sharing.dto.membership.onboarding.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MembershipOnboardingResponseMapper {
    public MembershipProfileResponse notStarted(NhaTruong school) {
        return new MembershipProfileResponse(null, "STUDENT", "NOT_STARTED", null, school(school),
                null, null, null, null, List.of(), null, "COMPLETE_PROFILE");
    }

    public MembershipProfileResponse profile(HoSoSinhVien profile, List<BangChungThanhVien> evidence,
            LanNopHoSoThanhVien submission) {
        return new MembershipProfileResponse(profile.getId(), "STUDENT", profile.getTrangThaiHoSo().name(), profile.getVersion(),
                school(profile.getNhaTruong()), profile.getMaSoSinhVien(), studying(profile), profile.getNgayNhapHoc(),
                clazz(profile.getLop()), evidence(evidence), submission(submission),
                submission == null ? "COMPLETE_PROFILE" : "WAIT_FOR_REVIEW");
    }

    public MembershipSubmissionResponse submission(HoSoSinhVien profile, List<BangChungThanhVien> evidence,
            LanNopHoSoThanhVien submission, boolean created) {
        return new MembershipSubmissionResponse(profile.getId(), "STUDENT", profile.getTrangThaiHoSo().name(), profile.getVersion(),
                school(profile.getNhaTruong()), profile.getMaSoSinhVien(), submission.getId(), submission.getLanNop(),
                submission.getNopLuc(), evidence(evidence), "WAIT_FOR_REVIEW", "CHO_DUYET_HO_SO", created);
    }

    private static MembershipSchoolResponse school(NhaTruong school) {
        return new MembershipSchoolResponse(school.getId(), school.getMaTruong(), school.getTenTruong());
    }

    private static MembershipClassResponse clazz(Lop lop) {
        return lop == null ? null : new MembershipClassResponse(lop.getId(), lop.getMaLop());
    }

    private static Boolean studying(HoSoSinhVien profile) {
        return profile.getTrangThaiHocTap() == null ? null : profile.getTrangThaiHocTap() == TrangThaiHocTap.DANG_HOC;
    }

    private static List<MembershipEvidenceResponse> evidence(Collection<BangChungThanhVien> rows) {
        if (rows == null) return List.of();
        return rows.stream().filter(BangChungThanhVien::isCurrent)
                .sorted(Comparator.comparing(e -> e.getSlot().name()))
                .map(e -> new MembershipEvidenceResponse(e.getId(), e.getSlot(), e.getOriginalFilename(),
                        e.getVerifiedMediaType(), e.getSizeBytes(), e.getCreatedAt()))
                .toList();
    }

    private static MembershipSubmissionSummaryResponse submission(LanNopHoSoThanhVien row) {
        return row == null ? null : new MembershipSubmissionSummaryResponse(row.getId(), row.getLanNop(), row.getNopLuc());
    }
}
