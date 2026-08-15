package com.zanh.route_sharing.dto.trip.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SafetyIncidentReassignRequest(
        @NotNull @Positive(message = "newHandlerUserId phải là số dương.") Long newHandlerUserId,
        @NotBlank(message = "reason không được trống.") @Size(max=1000) String reason) {}
