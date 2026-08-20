package com.zanh.route_sharing.dto.auth.session;

import com.zanh.route_sharing.security.IssuedToken;
import com.zanh.route_sharing.security.TokenPair;

import java.time.Instant;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    public static AuthTokenResponse onboarding(IssuedToken token) {
        return new AuthTokenResponse("Bearer", token.value(), token.expiresAt(), null, null);
    }

    public static AuthTokenResponse full(TokenPair pair) {
        return new AuthTokenResponse(
                pair.tokenType(),
                pair.accessToken(),
                pair.accessTokenExpiresAt(),
                pair.refreshToken(),
                pair.refreshTokenExpiresAt());
    }
}
