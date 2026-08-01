package com.zanh.route_sharing.security;

import java.time.Instant;

public record TokenPair(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
