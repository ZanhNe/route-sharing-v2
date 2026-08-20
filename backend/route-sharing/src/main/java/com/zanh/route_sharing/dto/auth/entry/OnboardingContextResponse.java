package com.zanh.route_sharing.dto.auth.entry;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

public record OnboardingContextResponse(
        Long accountId,
        String fullName,
        String schoolEmail,
        String accountStatus,
        boolean emailVerified,
        String sessionMode,
        String nextAction) {

    public static OnboardingContextResponse from(NguoiDung account) {
        TrangThaiTaiKhoan status = account.getTrangThaiTaiKhoan();
        String nextAction = switch (status) {
            case CHO_XAC_THUC_EMAIL -> "VERIFY_EMAIL";
            case CHO_DUYET_HO_SO -> "COMPLETE_PROFILE";
            default -> throw new IllegalStateException("Account không còn ở onboarding state: " + status);
        };
        return new OnboardingContextResponse(
                account.getId(),
                account.getHoTen(),
                account.getEmailTruong(),
                status.name(),
                account.getEmailDaXacThucLuc() != null,
                "ONBOARDING",
                nextAction);
    }
}
