package com.zanh.route_sharing.security.boarding.model;

import java.util.Arrays;

public record ProtectedBoardingCode(byte[] encryptedCode, byte[] nonce, String keyVersion) {
    public ProtectedBoardingCode {
        if (encryptedCode == null || encryptedCode.length == 0
                || nonce == null || nonce.length != 12
                || keyVersion == null || keyVersion.isBlank() || keyVersion.length() > 64) {
            throw new IllegalArgumentException("ProtectedBoardingCode không hợp lệ.");
        }
        encryptedCode = Arrays.copyOf(encryptedCode, encryptedCode.length);
        nonce = Arrays.copyOf(nonce, nonce.length);
        keyVersion = keyVersion.trim();
    }

    @Override
    public byte[] encryptedCode() {
        return Arrays.copyOf(encryptedCode, encryptedCode.length);
    }

    @Override
    public byte[] nonce() {
        return Arrays.copyOf(nonce, nonce.length);
    }
}
