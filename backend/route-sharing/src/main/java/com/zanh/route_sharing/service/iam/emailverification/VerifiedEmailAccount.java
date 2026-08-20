package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

import java.time.Instant;

public record VerifiedEmailAccount(
        Long accountId,
        String email,
        TrangThaiTaiKhoan accountStatus,
        long securityVersion,
        Instant verifiedAt) {
}
