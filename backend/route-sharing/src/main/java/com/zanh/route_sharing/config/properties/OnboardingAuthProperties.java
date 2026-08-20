package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.onboarding-auth")
public class OnboardingAuthProperties {
    @NotNull(message = "app.onboarding-auth.access-token-ttl không được để trống")
    private Duration accessTokenTtl = Duration.ofMinutes(30);

    @AssertTrue(message = "app.onboarding-auth.access-token-ttl không hợp lệ")
    public boolean isDurationConfigurationValid() {
        return accessTokenTtl != null
                && !accessTokenTtl.isZero()
                && !accessTokenTtl.isNegative()
                && accessTokenTtl.compareTo(Duration.ofHours(2)) <= 0;
    }
}
