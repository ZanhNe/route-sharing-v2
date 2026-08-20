package com.zanh.route_sharing.dto.auth.registration;

import java.util.List;

public record RegistrationLegalContextResponse(
        Long schoolId,
        String schoolName,
        List<RegistrationLegalDocumentResponse> documents) {
}
