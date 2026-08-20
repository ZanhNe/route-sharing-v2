package com.zanh.route_sharing.dto.complaint.review;

import jakarta.validation.constraints.*;

public record ComplaintResponseRequest(@NotBlank @Size(min = 20, max = 5000) String content) {
}
