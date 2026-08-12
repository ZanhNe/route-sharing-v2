package com.zanh.route_sharing.dto.trip.pickuparrival;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TripPickupArrivalLocationRequest(
        @NotNull(message = "latitude không được trống.")
        @DecimalMin(value = "-90.0", message = "latitude phải từ -90 đến 90.")
        @DecimalMax(value = "90.0", message = "latitude phải từ -90 đến 90.")
        BigDecimal latitude,
        @NotNull(message = "longitude không được trống.")
        @DecimalMin(value = "-180.0", message = "longitude phải từ -180 đến 180.")
        @DecimalMax(value = "180.0", message = "longitude phải từ -180 đến 180.")
        BigDecimal longitude) {
}
