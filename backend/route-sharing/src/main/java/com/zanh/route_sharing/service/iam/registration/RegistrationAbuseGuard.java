package com.zanh.route_sharing.service.iam.registration;

public interface RegistrationAbuseGuard {
    void check(String remoteAddress, String normalizedEmail);
}
