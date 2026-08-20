package com.zanh.route_sharing.dto.auth.registration;

public record RegistrationSchoolResponse(
        Long schoolId,
        String schoolCode,
        String schoolName,
        String abbreviation,
        String logoUrl) {
}
