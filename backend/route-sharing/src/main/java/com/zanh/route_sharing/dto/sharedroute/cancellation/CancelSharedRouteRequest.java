package com.zanh.route_sharing.dto.sharedroute.cancellation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelSharedRouteRequest(
        @NotBlank(message = "Lý do hủy không được để trống.")
        @Size(max = 2000, message = "Lý do hủy không được vượt quá 2000 ký tự.")
        String reason) {
}
