package com.zanh.route_sharing.integration.mail;

import com.zanh.route_sharing.config.properties.EmailVerificationMailProperties;
import com.zanh.route_sharing.config.properties.EmailVerificationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpVerificationEmailSender implements VerificationEmailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailVerificationMailProperties mailProperties;
    private final EmailVerificationProperties verificationProperties;

    public SmtpVerificationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailVerificationMailProperties mailProperties,
            EmailVerificationProperties verificationProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.verificationProperties = verificationProperties;
    }

    @Override
    public void sendVerificationCode(String destination, String code) {
        String from = mailProperties.getFrom();
        if (from == null || from.isBlank()) {
            throw configurationFailure("SMTP sender address is not configured.");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw configurationFailure("SMTP mail sender is unavailable.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from.trim());
        message.setTo(destination);
        message.setSubject(mailProperties.getSubject());
        long validityMinutes = verificationProperties.getTtl().toMinutes();
        message.setText("Mã xác thực email RouteShare của bạn là: " + code
                + "\nMã có hiệu lực trong " + validityMinutes + " phút. Không chia sẻ mã này cho người khác.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new VerificationEmailDeliveryException(
                    VerificationEmailFailureCategory.TEMPORARY_UNAVAILABLE,
                    "Verification email provider is temporarily unavailable.",
                    exception);
        }
    }

    private static VerificationEmailDeliveryException configurationFailure(String message) {
        return new VerificationEmailDeliveryException(
                VerificationEmailFailureCategory.AUTHENTICATION_OR_CONFIGURATION_FAILURE,
                message);
    }
}
