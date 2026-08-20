package com.zanh.route_sharing.dto.complaint.review;

import jakarta.validation.constraints.*;

public record ComplaintReassignRequest(@NotNull @Positive Long newReviewerId,
                @NotBlank @Size(min = 10, max = 1000) String reason) {
}
