package com.zanh.route_sharing.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.email-verification-protection")
public class EmailVerificationProtectionProperties {
    private String activeKeyVersion = "v1";
    private Map<String, String> keys = new LinkedHashMap<>();

    public String getActiveKeyVersion() {
        return activeKeyVersion;
    }

    public void setActiveKeyVersion(String activeKeyVersion) {
        this.activeKeyVersion = activeKeyVersion;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keys);
    }
}
