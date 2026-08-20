package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

import java.time.Instant;

public record VerifiedAccountCredential(
                Long accountId,
                String fullName,
                String schoolEmail,
                TrangThaiTaiKhoan accountStatus,
                Instant emailVerifiedAt,
                long securityVersion) {
}
