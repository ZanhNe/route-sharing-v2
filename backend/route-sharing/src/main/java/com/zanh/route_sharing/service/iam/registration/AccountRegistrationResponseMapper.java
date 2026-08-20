package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountRegistrationResponseMapper {
    public AccountRegistrationResponse toResponse(NguoiDung account) {
        return new AccountRegistrationResponse(
                account.getId(),
                account.getHoTen(),
                account.getEmailTruong(),
                account.getTrangThaiTaiKhoan().name(),
                account.getEmailDaXacThucLuc() != null,
                account.getCreatedAt(),
                "VERIFY_EMAIL");
    }
}
