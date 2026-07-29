package com.zanh.route_sharing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "goong")
@Configuration
@Getter
@Setter
public class GoongConfig {
    private String apiKey;
    private String baseUrl;
    private String url;

    public String getBaseUrl() {
        String resolved = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : url;
        if (resolved == null || resolved.isBlank()) {
            return resolved;
        }
        return resolved.endsWith("/") ? resolved : resolved + "/";
    }
}