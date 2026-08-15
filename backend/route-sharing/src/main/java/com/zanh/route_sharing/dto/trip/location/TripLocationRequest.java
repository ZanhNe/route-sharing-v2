package com.zanh.route_sharing.dto.trip.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record TripLocationRequest(
        @NotNull(message = "position không được trống.")
        @Valid
        TripLocationPointRequest position,
        @NotNull(message = "observedAt không được trống.")
        Instant observedAt,
        @DecimalMin(value = "0.0", inclusive = true, message = "accuracyMeters phải >= 0.")
        BigDecimal accuracyMeters) {
}
