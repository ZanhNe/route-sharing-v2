package com.zanh.route_sharing.security;

import java.time.Instant;

public record JwtRefreshClaims(Long userId, String email, String jwtId, Instant expiresAt) {
}
