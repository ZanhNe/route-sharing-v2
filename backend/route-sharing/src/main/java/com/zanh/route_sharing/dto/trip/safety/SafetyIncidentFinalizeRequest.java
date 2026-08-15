package com.zanh.route_sharing.dto.trip.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SafetyIncidentFinalizeRequest(
        @NotNull(message = "outcome không được trống.") String outcome,
        @NotBlank(message = "safeConclusion không được trống.")
        @Size(max = 5000, message = "safeConclusion không được vượt quá 5000 ký tự.") String safeConclusion) {}
