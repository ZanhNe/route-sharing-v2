package com.zanh.route_sharing.service.iam.emailverification;

public interface EmailVerificationAbuseGuard {
    void checkRequest(Long accountId, String remoteAddress);

    void checkVerify(Long accountId, String remoteAddress);
}
