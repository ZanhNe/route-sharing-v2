package com.zanh.route_sharing.dto.complaint.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitComplaintRequest(
                @NotBlank(message = "title không được trống.") @Size(max = 255, message = "title không được vượt quá 255 ký tự.") String title,
                @NotBlank(message = "content không được trống.") @Size(max = 5000, message = "content không được vượt quá 5000 ký tự.") String content,
                @Positive(message = "incidentId phải là số dương.") Long incidentId) {
}
