package com.zanh.route_sharing.config.properties;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared validation for configured HTTP(S) origins.
 *
 * <p>This is deliberately independent from CORS and WebSocket configuration so
 * both boundaries apply exactly the same origin rules without depending on one
 * another.</p>
 */
final class OriginListValidator {

    private OriginListValidator() {
    }

    static boolean isValid(List<String> origins) {
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
                if (!isHttpOrigin(uri) || !normalized.add(normalizeOrigin(uri))) {
                    return false;
                }
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHttpOrigin(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && (uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                && uri.getQuery() == null
                && uri.getFragment() == null;
    }

    private static String normalizeOrigin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port < 0
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (defaultPort ? "" : ":" + port);
    }
}
