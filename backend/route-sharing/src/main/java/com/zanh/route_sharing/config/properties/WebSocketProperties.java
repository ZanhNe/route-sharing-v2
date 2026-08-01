package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {
    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://localhost:8080"));

    @AssertTrue(message = "app.websocket.allowed-origins không được để trống")
    public boolean isAllowedOriginsValid() {
        return areValidOrigins(allowedOrigins);
    }

    static boolean areValidOrigins(List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            return false;
        }
        Set<String> normalized = new HashSet<>();
        for (String value : origins) {
            if (value == null || value.isBlank() || "*".equals(value.trim())) {
                return false;
            }
            try {
                URI uri = URI.create(value.trim());
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null
                        || uri.getUserInfo() != null
                        || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))
                        || uri.getQuery() != null
                        || uri.getFragment() != null
                        || !normalized.add(normalizeOrigin(uri))) {
                    return false;
                }
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeOrigin(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();
        boolean defaultPort = port < 0
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + uri.getHost().toLowerCase()
                + (defaultPort ? "" : ":" + port);
    }
}
