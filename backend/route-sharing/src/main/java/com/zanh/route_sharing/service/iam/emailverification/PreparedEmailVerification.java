package com.zanh.route_sharing.service.iam.emailverification;

public record PreparedEmailVerification(
        Long accountId,
        Long challengeId,
        String destination,
        String code) {
}
