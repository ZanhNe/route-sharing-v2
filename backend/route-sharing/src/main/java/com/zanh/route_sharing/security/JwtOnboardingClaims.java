package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

import java.time.Instant;

public record JwtOnboardingClaims(
                Long userId,
                String email,
                TrangThaiTaiKhoan accountStatus,
                long securityVersion,
                OnboardingStep step,
                String jwtId,
                Instant expiresAt) {
}
