package com.zanh.route_sharing.storage.evidence;

import org.springframework.core.io.Resource;

public record VerifiedBinary(Resource resource, long sizeBytes, String sha256Hex) {
    public VerifiedBinary {
        if (resource == null || sizeBytes <= 0 || sha256Hex == null)
            throw new IllegalArgumentException("Verified binary không hợp lệ.");
    }
}
