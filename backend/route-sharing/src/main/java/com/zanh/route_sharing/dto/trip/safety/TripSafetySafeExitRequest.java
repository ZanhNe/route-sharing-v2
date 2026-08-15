package com.zanh.route_sharing.dto.trip.safety;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TripSafetySafeExitRequest(@NotNull @Valid Position location) {
    public record Position(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) {}
}
