package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.service.membership.onboarding.model.PreparedMembershipEvidence;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class MembershipMaterialComparator {
    public boolean matches(LanNopHoSoThanhVien submission, String studentCode, TrangThaiHocTap studyState,
            LocalDate enrollmentDate, Long classId, Long schoolId,
            List<BangChungThanhVien> currentEvidence, List<PreparedMembershipEvidence> prepared,
            Set<ViTriBangChungThanhVien> removals) {
        if (submission == null || submission.getLanNop() != 1) return false;
        if (!Objects.equals(submission.getStudentCodeSnapshot(), studentCode)) return false;
        if (submission.isCurrentlyStudyingSnapshot() != (studyState == TrangThaiHocTap.DANG_HOC)) return false;
        if (!Objects.equals(submission.getNgayNhapHocSnapshot(), enrollmentDate)) return false;
        if (!Objects.equals(submission.getLopIdSnapshot(), classId)) return false;
        if (!Objects.equals(submission.getSchoolIdSnapshot(), schoolId)) return false;
        if (!Objects.equals(submission.getPolicyKey(), MembershipPolicy.POLICY_KEY)
                || submission.getPolicyVersion() != MembershipPolicy.POLICY_VERSION) return false;
        return submittedHashes(submission).equals(effectiveHashes(currentEvidence, prepared, removals));
    }

    private static Map<ViTriBangChungThanhVien, String> submittedHashes(LanNopHoSoThanhVien submission) {
        EnumMap<ViTriBangChungThanhVien, String> result = new EnumMap<>(ViTriBangChungThanhVien.class);
        submission.getBangChungDaNop().forEach(e -> result.put(e.getSlot(), e.getSha256()));
        return result;
    }

    private static Map<ViTriBangChungThanhVien, String> effectiveHashes(List<BangChungThanhVien> current,
            List<PreparedMembershipEvidence> prepared, Set<ViTriBangChungThanhVien> removals) {
        EnumMap<ViTriBangChungThanhVien, String> result = new EnumMap<>(ViTriBangChungThanhVien.class);
        current.stream().filter(BangChungThanhVien::isCurrent).forEach(e -> result.put(e.getSlot(), e.getSha256()));
        removals.forEach(result::remove);
        prepared.forEach(p -> result.put(p.slot(), p.staged().sha256Hex()));
        return result;
    }
}
