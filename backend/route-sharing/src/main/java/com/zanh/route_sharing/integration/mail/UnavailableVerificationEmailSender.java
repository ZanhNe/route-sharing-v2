package com.zanh.route_sharing.integration.mail;

public class UnavailableVerificationEmailSender implements VerificationEmailSender {
    @Override
    public void sendVerificationCode(String destination, String code) {
        throw new VerificationEmailDeliveryException(
                VerificationEmailFailureCategory.AUTHENTICATION_OR_CONFIGURATION_FAILURE,
                "Verification email delivery is not configured.");
    }
}
