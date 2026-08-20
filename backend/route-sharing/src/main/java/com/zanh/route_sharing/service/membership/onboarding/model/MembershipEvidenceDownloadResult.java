package com.zanh.route_sharing.service.membership.onboarding.model;

import org.springframework.core.io.Resource;

public record MembershipEvidenceDownloadResult(Resource resource, String originalFilename,
        String verifiedMediaType, long sizeBytes) {}
