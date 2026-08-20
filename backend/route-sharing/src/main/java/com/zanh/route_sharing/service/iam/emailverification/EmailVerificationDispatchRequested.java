package com.zanh.route_sharing.service.iam.emailverification;

public record EmailVerificationDispatchRequested(
        Long accountId,
        Long challengeId,
        String destination,
        String code) {
}
