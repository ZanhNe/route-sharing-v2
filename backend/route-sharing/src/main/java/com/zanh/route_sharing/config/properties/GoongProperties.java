package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "goong")
public class GoongProperties {
    @NotBlank(message = "goong.api-key không được để trống")
    private String apiKey;
    @NotNull(message = "goong.base-url không được để trống")
    private URI baseUrl = URI.create("https://rsapi.goong.io");

    @AssertTrue(message = "goong.base-url phải là một HTTPS origin không có thông tin xác thực, query hoặc fragment")
    public boolean isSecureBaseUrl() {
        if (baseUrl == null
                || !"https".equalsIgnoreCase(baseUrl.getScheme())
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null) {
            return false;
        }
        String path = baseUrl.getPath();
        return path == null || path.isEmpty() || "/".equals(path);
    }
}
