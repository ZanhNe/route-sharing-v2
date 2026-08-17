package com.zanh.route_sharing.security.dropoff.model;

import java.util.Arrays;

public record ProtectedDropoffCode(byte[] encryptedCode, byte[] nonce, String keyVersion) {
    public ProtectedDropoffCode {
        if (encryptedCode == null || encryptedCode.length == 0 || nonce == null || nonce.length != 12
                || keyVersion == null || keyVersion.isBlank() || keyVersion.length() > 64) {
            throw new IllegalArgumentException("ProtectedDropoffCode không hợp lệ.");
        }
        encryptedCode = Arrays.copyOf(encryptedCode, encryptedCode.length);
        nonce = Arrays.copyOf(nonce, nonce.length);
        keyVersion = keyVersion.trim();
    }
    @Override public byte[] encryptedCode() { return Arrays.copyOf(encryptedCode, encryptedCode.length); }
    @Override public byte[] nonce() { return Arrays.copyOf(nonce, nonce.length); }
}
