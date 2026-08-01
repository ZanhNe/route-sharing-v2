package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

import java.time.Instant;
import java.util.List;

public record JwtAccessClaims(
        Long userId,
        String email,
        TrangThaiTaiKhoan accountStatus,
        long securityVersion,
        List<String> authorities,
        String jwtId,
        Instant expiresAt
) {
}
