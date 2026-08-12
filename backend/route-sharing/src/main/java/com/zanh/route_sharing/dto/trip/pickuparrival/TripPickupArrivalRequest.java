package com.zanh.route_sharing.dto.trip.pickuparrival;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TripPickupArrivalRequest(
        @NotNull(message = "currentLocation không được trống.")
        @Valid
        TripPickupArrivalLocationRequest currentLocation) {
}
