package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.email-verification-abuse")
public class EmailVerificationAbuseProperties {
    @NotNull
    private Duration requestWindow = Duration.ofMinutes(15);
    @Min(1)
    @Max(10000)
    private int requestPerAccount = 5;
    @Min(1)
    @Max(10000)
    private int requestPerIp = 30;
    @NotNull
    private Duration verifyWindow = Duration.ofMinutes(10);
    @Min(1)
    @Max(10000)
    private int verifyPerAccount = 10;
    @Min(1)
    @Max(10000)
    private int verifyPerIp = 60;
    @Min(100)
    @Max(100000)
    private int maxTrackedKeys = 10000;

    public Duration getRequestWindow() {
        return requestWindow;
    }

    public void setRequestWindow(Duration requestWindow) {
        this.requestWindow = requestWindow;
    }

    public int getRequestPerAccount() {
        return requestPerAccount;
    }

    public void setRequestPerAccount(int requestPerAccount) {
        this.requestPerAccount = requestPerAccount;
    }

    public int getRequestPerIp() {
        return requestPerIp;
    }

    public void setRequestPerIp(int requestPerIp) {
        this.requestPerIp = requestPerIp;
    }

    public Duration getVerifyWindow() {
        return verifyWindow;
    }

    public void setVerifyWindow(Duration verifyWindow) {
        this.verifyWindow = verifyWindow;
    }

    public int getVerifyPerAccount() {
        return verifyPerAccount;
    }

    public void setVerifyPerAccount(int verifyPerAccount) {
        this.verifyPerAccount = verifyPerAccount;
    }

    public int getVerifyPerIp() {
        return verifyPerIp;
    }

    public void setVerifyPerIp(int verifyPerIp) {
        this.verifyPerIp = verifyPerIp;
    }

    public int getMaxTrackedKeys() {
        return maxTrackedKeys;
    }

    public void setMaxTrackedKeys(int maxTrackedKeys) {
        this.maxTrackedKeys = maxTrackedKeys;
    }

    @AssertTrue(message = "app.email-verification-abuse windows phải dương")
    public boolean isWindowConfigurationValid() {
        return positive(requestWindow) && positive(verifyWindow);
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
