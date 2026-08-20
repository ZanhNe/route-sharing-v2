package com.zanh.route_sharing.integration.mail;

public interface VerificationEmailSender {
    void sendVerificationCode(String destination, String code);
}
