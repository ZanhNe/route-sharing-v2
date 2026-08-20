package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.dto.membership.onboarding.MembershipProfileDraftRequest;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MembershipPolicy {
    public static final String POLICY_KEY = "MEMBERSHIP_STUDENT_INITIAL";
    public static final int POLICY_VERSION = 1;
    private static final Set<String> MEMBERSHIP_MEDIA_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");

    public void requireAccountState(NguoiDung account) {
        if (account == null || account.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.CHO_DUYET_HO_SO) {
            throw new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_PREDECESSOR_INVARIANT_VIOLATION",
                    "Tài khoản không còn ở trạng thái hoàn tất hồ sơ.");
        }
    }

    public void requireStudentProfile(HoSoThanhVien profile) {
        if (profile != null && !(profile instanceof HoSoSinhVien)) {
            throw new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_CURRENT_LIFECYCLE_CONFLICT",
                    "Đã tồn tại một vòng đời hồ sơ thành viên không tương thích.");
        }
    }

    public void requireDraftEditable(HoSoSinhVien profile, Long expectedVersion) {
        if (profile == null) {
            if (expectedVersion != null) throw concurrent();
            return;
        }
        if (profile.getTrangThaiHoSo() != TrangThaiHoSoThanhVien.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_PROFILE_ALREADY_SUBMITTED",
                    "Hồ sơ ban đầu đã được nộp và không thể sửa bằng MEM-01.");
        }
        if (expectedVersion == null || !Objects.equals(expectedVersion, profile.getVersion())) throw concurrent();
    }

    public String normalizeStudentCode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 100) {
            throw validation("studentCode không được vượt quá 100 ký tự.");
        }
        return normalized;
    }

    public TrangThaiHocTap toStudyState(Boolean currentlyStudying, TrangThaiHocTap current) {
        if (currentlyStudying == null) return current;
        // MEM-01 only owns the applicant's current-student claim. A negative claim must not
        // invent a stronger academic lifecycle state such as THOI_HOC/DA_TOT_NGHIEP.
        return currentlyStudying ? TrangThaiHocTap.DANG_HOC : null;
    }

    public void requireEvidenceMediaAllowed(String mediaType) {
        if (!MEMBERSHIP_MEDIA_TYPES.contains(mediaType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EVIDENCE_MEDIA_TYPE_NOT_ALLOWED",
                    "MEM-01 chỉ chấp nhận JPEG, PNG hoặc PDF.");
        }
    }

    public void validateSlotMutation(MembershipProfileDraftRequest request, Set<ViTriBangChungThanhVien> suppliedSlots) {
        validateSlotMutation(request.removeEvidenceSlots(), suppliedSlots);
    }

    public void validateSlotMutation(Set<ViTriBangChungThanhVien> removals,
            Set<ViTriBangChungThanhVien> suppliedSlots) {
        Set<ViTriBangChungThanhVien> safeRemovals = removals == null ? Set.of() : removals;
        for (ViTriBangChungThanhVien slot : suppliedSlots) {
            if (safeRemovals.contains(slot)) {
                throw validation("Một evidence slot không thể vừa xóa vừa thay thế trong cùng request.");
            }
        }
    }

    public void requireEvidenceShape(Collection<BangChungThanhVien> evidence) {
        EnumSet<ViTriBangChungThanhVien> slots = EnumSet.noneOf(ViTriBangChungThanhVien.class);
        evidence.stream().filter(BangChungThanhVien::isCurrent).forEach(e -> slots.add(e.getSlot()));
        requireEvidenceShape(slots);
    }

    public void requireEvidenceShape(Set<ViTriBangChungThanhVien> slots) {
        if (slots.contains(ViTriBangChungThanhVien.STUDENT_CARD_BACK)
                && !slots.contains(ViTriBangChungThanhVien.STUDENT_CARD_FRONT)) {
            throw validation("Mặt sau thẻ sinh viên không thể tồn tại nếu thiếu mặt trước.");
        }
    }

    public void requireSubmitMaterial(HoSoSinhVien profile, Collection<BangChungThanhVien> evidence) {
        EnumSet<ViTriBangChungThanhVien> slots = EnumSet.noneOf(ViTriBangChungThanhVien.class);
        evidence.stream().filter(BangChungThanhVien::isCurrent).forEach(e -> slots.add(e.getSlot()));
        requireSubmitMaterial(profile, slots);
    }

    public void requireSubmitMaterial(HoSoSinhVien profile, Set<ViTriBangChungThanhVien> slots) {
        if (profile.getMaSoSinhVien() == null || profile.getMaSoSinhVien().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MEMBERSHIP_STUDENT_CODE_REQUIRED",
                    "Mã sinh viên là bắt buộc khi nộp hồ sơ.");
        }
        if (profile.getTrangThaiHocTap() != TrangThaiHocTap.DANG_HOC) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MEMBERSHIP_CURRENT_STUDENT_REQUIRED",
                    "Người nộp phải xác nhận hiện đang là sinh viên.");
        }
        requireEvidenceShape(slots);
        boolean card = slots.contains(ViTriBangChungThanhVien.STUDENT_CARD_FRONT);
        boolean confirmation = slots.contains(ViTriBangChungThanhVien.OFFICIAL_STUDENT_CONFIRMATION);
        if (!card && !confirmation) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MEMBERSHIP_EVIDENCE_REQUIRED",
                    "Cần thẻ sinh viên hoặc giấy xác nhận sinh viên chính thức.");
        }
    }

    public void requireSchoolActive(NhaTruong school) {
        if (school == null || !Boolean.TRUE.equals(school.getDangHoatDong())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SCHOOL_ONBOARDING_UNAVAILABLE",
                    "Nhà trường hiện không tiếp nhận hồ sơ onboarding mới.");
        }
    }

    public BusinessException classSchoolMismatch() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "MEMBERSHIP_CLASS_SCHOOL_MISMATCH",
                "Lớp không thuộc nhà trường của hồ sơ.");
    }

    public BusinessException identifierUnavailable() {
        return new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_IDENTIFIER_UNAVAILABLE",
                "Mã sinh viên hiện không khả dụng trong nhà trường này.");
    }

    private static BusinessException concurrent() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Hồ sơ đã thay đổi. Vui lòng tải lại trước khi lưu.");
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
