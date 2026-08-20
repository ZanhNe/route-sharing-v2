package com.zanh.route_sharing.config;

import com.zanh.route_sharing.config.properties.EmailVerificationMailProperties;
import com.zanh.route_sharing.config.properties.EmailVerificationProperties;
import com.zanh.route_sharing.integration.mail.SmtpVerificationEmailSender;
import com.zanh.route_sharing.integration.mail.UnavailableVerificationEmailSender;
import com.zanh.route_sharing.integration.mail.VerificationEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
public class VerificationEmailConfiguration {
    @Bean
    VerificationEmailSender verificationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailVerificationMailProperties mailProperties,
            EmailVerificationProperties verificationProperties) {
        if (!mailProperties.isEnabled()) {
            return new UnavailableVerificationEmailSender();
        }
        return new SmtpVerificationEmailSender(mailSenderProvider, mailProperties, verificationProperties);
    }
}
