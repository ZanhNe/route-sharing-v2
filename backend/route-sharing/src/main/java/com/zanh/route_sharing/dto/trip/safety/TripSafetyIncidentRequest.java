package com.zanh.route_sharing.dto.trip.safety;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TripSafetyIncidentRequest(
        @NotNull(message = "type không được trống.") LoaiSuCo type,
        @Size(max = 5000, message = "description không được vượt quá 5000 ký tự.") String description,
        @Positive(message = "reportedParticipantId phải là số dương.") Long reportedParticipantId,
        Boolean emergencyActionConfirmed) {

}
