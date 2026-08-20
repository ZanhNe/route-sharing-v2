package com.zanh.route_sharing.repository.membership.onboarding.model;

public record MembershipEvidenceBinaryMetadata(Long evidenceId, String originalFilename, String verifiedMediaType,
        long sizeBytes, String sha256, String storageKey) {}
