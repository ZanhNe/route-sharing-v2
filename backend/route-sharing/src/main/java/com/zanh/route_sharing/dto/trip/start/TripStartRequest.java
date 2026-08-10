package com.zanh.route_sharing.dto.trip.start;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TripStartRequest(
        @NotNull(message = "currentLocation không được trống.")
        @Valid
        TripStartLocationRequest currentLocation) {
}
