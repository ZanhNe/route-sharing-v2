package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationRequestResponse;
import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationResponse;

public interface EmailVerificationService {
    EmailVerificationRequestResponse requestCode(Long accountId, String remoteAddress);

    EmailVerificationResponse verifyCode(Long accountId, String code, String remoteAddress);
}
