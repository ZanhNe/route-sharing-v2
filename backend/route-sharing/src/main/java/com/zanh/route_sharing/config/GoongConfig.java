package com.zanh.route_sharing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "goong")
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