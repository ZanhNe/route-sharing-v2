package com.zanh.route_sharing.dto.auth;

import com.zanh.route_sharing.security.TokenPair;

import java.time.Instant;

public record TokenResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(pair.tokenType(), pair.accessToken(), pair.accessTokenExpiresAt(),
                pair.refreshToken(), pair.refreshTokenExpiresAt());
    }
}
