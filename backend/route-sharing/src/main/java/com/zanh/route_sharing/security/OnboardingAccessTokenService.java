package com.zanh.route_sharing.security;

import com.zanh.route_sharing.config.properties.OnboardingAuthProperties;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingAccessTokenService {
    private final JwtUtil jwtUtil;
    private final OnboardingAuthProperties properties;

    public IssuedToken issue(Long userId,
            String email,
            TrangThaiTaiKhoan accountStatus,
            long securityVersion,
            OnboardingStep step) {
        return jwtUtil.issueOnboardingToken(
                userId,
                email,
                accountStatus,
                securityVersion,
                step,
                properties.getAccessTokenTtl());
    }

    public JwtOnboardingClaims parse(String rawToken) {
        return jwtUtil.parseOnboardingToken(rawToken);
    }
}
