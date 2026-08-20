package com.zanh.route_sharing.dto.auth.registration;

import java.time.Instant;

public record RegistrationLegalDocumentResponse(
        Long documentId,
        String documentCode,
        String type,
        String title,
        String version,
        String contentUrl,
        boolean required,
        Instant effectiveFrom) {
}
