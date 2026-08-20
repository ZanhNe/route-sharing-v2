package com.zanh.route_sharing.dto.auth.registration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AccountRegistrationRequest(
        @NotNull @Positive Long schoolId,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String schoolEmail,
        @NotNull String password,
        @NotNull @Size(max = 50) List<@NotNull @Positive Long> acceptedLegalDocumentIds) {
}
