package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RegistrationPasswordPolicy {

    public void validate(String rawPassword) {
        if (rawPassword == null
                || rawPassword.codePoints().count() < 8
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || rawPassword.codePoints().allMatch(Character::isWhitespace)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_PASSWORD",
                    "Mật khẩu đăng ký không đáp ứng chính sách bảo mật.");
        }
    }
}
