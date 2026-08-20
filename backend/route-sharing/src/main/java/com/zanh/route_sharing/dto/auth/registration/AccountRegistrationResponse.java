package com.zanh.route_sharing.dto.auth.registration;

import java.time.Instant;

public record AccountRegistrationResponse(
        Long accountId,
        String fullName,
        String schoolEmail,
        String accountStatus,
        boolean emailVerified,
        Instant registeredAt,
        String nextAction) {
}
