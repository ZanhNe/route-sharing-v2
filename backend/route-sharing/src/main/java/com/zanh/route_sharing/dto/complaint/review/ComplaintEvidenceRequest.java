package com.zanh.route_sharing.dto.complaint.review;

import jakarta.validation.constraints.*;

public record ComplaintEvidenceRequest(@NotNull @Positive Long targetParticipantId,
                @NotBlank @Size(min = 10, max = 1000) String reason) {
}
