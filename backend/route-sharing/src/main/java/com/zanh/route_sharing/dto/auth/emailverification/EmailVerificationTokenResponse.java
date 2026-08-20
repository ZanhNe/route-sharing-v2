package com.zanh.route_sharing.dto.auth.emailverification;

import java.time.Instant;

public record EmailVerificationTokenResponse(
                String tokenType,
                String accessToken,
                Instant accessTokenExpiresAt) {
}
