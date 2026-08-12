package com.zanh.route_sharing.dto.trip.boarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TripBoardingRequest(
        @NotBlank(message = "boardingCode không được trống.")
        @Pattern(regexp = "[0-9]{6}", message = "boardingCode phải gồm đúng 6 chữ số.")
        String boardingCode) {
}
