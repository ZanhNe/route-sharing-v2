package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    @NotBlank(message = "app.jwt.base64-secret không được để trống")
    private String base64Secret;
    @NotBlank(message = "app.jwt.issuer không được để trống")
    private String issuer = "route-sharing";
    @NotBlank(message = "app.jwt.audience không được để trống")
    private String audience = "route-sharing-api";
    @NotNull(message = "app.jwt.access-token-ttl không được để trống")
    private Duration accessTokenTtl = Duration.ofMinutes(20);
    @NotNull(message = "app.jwt.refresh-token-ttl không được để trống")
    private Duration refreshTokenTtl = Duration.ofDays(7);
    @NotNull(message = "app.jwt.clock-skew không được để trống")
    private Duration clockSkew = Duration.ofSeconds(30);

    @AssertTrue(message = "Cấu hình thời gian sống của token không hợp lệ")
    public boolean isDurationConfigurationValid() {
        return accessTokenTtl != null && !accessTokenTtl.isZero() && !accessTokenTtl.isNegative()
                && refreshTokenTtl != null && refreshTokenTtl.compareTo(accessTokenTtl) > 0
                && clockSkew != null && !clockSkew.isNegative() && clockSkew.compareTo(Duration.ofMinutes(5)) <= 0;
    }
}
