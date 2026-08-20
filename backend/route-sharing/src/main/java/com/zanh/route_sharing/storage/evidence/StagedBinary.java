package com.zanh.route_sharing.storage.evidence;

import java.nio.file.Path;

public record StagedBinary(Path path, long sizeBytes, String sha256Hex) {
    public StagedBinary {
        if (path == null || sizeBytes <= 0 || sha256Hex == null || !sha256Hex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Staged binary không hợp lệ.");
        }
    }
}
