package com.zanh.route_sharing.dto.trip.dropoffverification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TripDropoffVerificationRequest(
                @NotBlank(message = "dropoffCode không được trống.") @Pattern(regexp = "[0-9]{6}", message = "dropoffCode phải gồm đúng 6 chữ số.") String dropoffCode) {
}
