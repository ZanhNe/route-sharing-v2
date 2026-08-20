package com.zanh.route_sharing.service.iam.registration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.registration-abuse")
public class RegistrationAbuseProperties {
    @NotNull
    private Duration window = Duration.ofMinutes(10);
    @Min(1) @Max(10000)
    private int perIpAttempts = 30;
    @Min(1) @Max(10000)
    private int perEmailAttempts = 5;
    @Min(100) @Max(100000)
    private int maxTrackedKeys = 10000;

    @AssertTrue(message = "app.registration-abuse.window phải lớn hơn 0")
    public boolean isWindowPositive() {
        return window != null && !window.isZero() && !window.isNegative();
    }
}
