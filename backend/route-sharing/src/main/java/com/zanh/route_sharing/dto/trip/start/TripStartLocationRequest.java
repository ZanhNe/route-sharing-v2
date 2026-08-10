package com.zanh.route_sharing.dto.trip.start;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TripStartLocationRequest(
        @NotNull(message = "latitude không được trống.")
        @DecimalMin(value = "-90.0", message = "latitude phải >= -90.")
        @DecimalMax(value = "90.0", message = "latitude phải <= 90.")
        BigDecimal latitude,
        @NotNull(message = "longitude không được trống.")
        @DecimalMin(value = "-180.0", message = "longitude phải >= -180.")
        @DecimalMax(value = "180.0", message = "longitude phải <= 180.")
        BigDecimal longitude) {
}
