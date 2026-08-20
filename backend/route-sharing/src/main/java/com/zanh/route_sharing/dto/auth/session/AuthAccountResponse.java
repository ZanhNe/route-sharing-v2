package com.zanh.route_sharing.dto.auth.session;

import com.zanh.route_sharing.service.iam.auth.VerifiedAccountCredential;

public record AuthAccountResponse(
        Long id,
        String fullName,
        String schoolEmail,
        String status,
        boolean emailVerified) {

    public static AuthAccountResponse from(VerifiedAccountCredential account) {
        return new AuthAccountResponse(
                account.accountId(),
                account.fullName(),
                account.schoolEmail(),
                account.accountStatus().name(),
                account.emailVerifiedAt() != null);
    }
}
