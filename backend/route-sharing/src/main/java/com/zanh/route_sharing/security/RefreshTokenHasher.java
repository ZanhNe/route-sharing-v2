package com.zanh.route_sharing.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RefreshTokenHasher {
    private RefreshTokenHasher() {
    }

    public static String sha256(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không hợp lệ", exception);
        }
    }

    public static boolean matches(String rawToken, String expectedHexHash) {
        byte[] actual = sha256(rawToken).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHexHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
