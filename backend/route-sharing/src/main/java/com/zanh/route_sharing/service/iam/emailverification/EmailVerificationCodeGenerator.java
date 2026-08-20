package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.config.properties.EmailVerificationProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class EmailVerificationCodeGenerator {
    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailVerificationProperties properties;

    public EmailVerificationCodeGenerator(EmailVerificationProperties properties) {
        this.properties = properties;
    }

    public String generate() {
        int length = properties.getCodeLength();
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
}
