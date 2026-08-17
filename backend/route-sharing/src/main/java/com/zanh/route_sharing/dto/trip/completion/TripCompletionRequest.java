package com.zanh.route_sharing.dto.trip.completion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TripCompletionRequest(
                @NotNull(message = "currentLocation không được trống.") @Valid TripCompletionLocationRequest currentLocation) {
}
