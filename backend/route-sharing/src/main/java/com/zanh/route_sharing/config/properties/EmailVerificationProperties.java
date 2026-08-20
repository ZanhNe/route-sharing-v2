package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.email-verification")
public class EmailVerificationProperties {
    @Min(6)
    @Max(6)
    private int codeLength = 6;
    @NotNull
    private Duration ttl = Duration.ofMinutes(10);
    @Min(1)
    @Max(20)
    private int maxWrongAttempts = 5;
    @NotNull
    private Duration resendCooldown = Duration.ofSeconds(60);
    @NotNull
    private Duration terminalRowRetention = Duration.ofDays(7);

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxWrongAttempts() {
        return maxWrongAttempts;
    }

    public void setMaxWrongAttempts(int maxWrongAttempts) {
        this.maxWrongAttempts = maxWrongAttempts;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }

    public Duration getTerminalRowRetention() {
        return terminalRowRetention;
    }

    public void setTerminalRowRetention(Duration terminalRowRetention) {
        this.terminalRowRetention = terminalRowRetention;
    }

    @AssertTrue(message = "app.email-verification ttl/resend-cooldown phải dương")
    public boolean isDurationConfigurationValid() {
        return positive(ttl) && positive(resendCooldown) && positive(terminalRowRetention);
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
