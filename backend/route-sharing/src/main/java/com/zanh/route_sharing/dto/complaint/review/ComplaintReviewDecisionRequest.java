package com.zanh.route_sharing.dto.complaint.review;

import jakarta.validation.constraints.*;

public record ComplaintReviewDecisionRequest(@NotBlank String outcome,
                @NotBlank @Size(max = 5000) String conclusion) {
}
