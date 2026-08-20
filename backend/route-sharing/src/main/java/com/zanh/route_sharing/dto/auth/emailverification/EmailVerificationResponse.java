package com.zanh.route_sharing.dto.auth.emailverification;

import java.time.Instant;

public record EmailVerificationResponse(
                String accountStatus,
                boolean emailVerified,
                Instant verifiedAt,
                String nextAction,
                EmailVerificationTokenResponse token) {
}
