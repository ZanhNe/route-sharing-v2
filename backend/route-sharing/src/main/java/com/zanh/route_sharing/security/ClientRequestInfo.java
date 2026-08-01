package com.zanh.route_sharing.security;

public record ClientRequestInfo(String ipAddress, String userAgent) {
    public ClientRequestInfo {
        ipAddress = truncate(ipAddress, 64);
        userAgent = truncate(userAgent, 500);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
