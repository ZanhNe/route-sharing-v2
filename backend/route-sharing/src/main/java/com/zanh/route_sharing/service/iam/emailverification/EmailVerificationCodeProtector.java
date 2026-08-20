package com.zanh.route_sharing.service.iam.emailverification;

public interface EmailVerificationCodeProtector {
    String protect(Long accountId, String email, String code);

    boolean matches(String protectedValue, Long accountId, String email, String candidateCode);
}
