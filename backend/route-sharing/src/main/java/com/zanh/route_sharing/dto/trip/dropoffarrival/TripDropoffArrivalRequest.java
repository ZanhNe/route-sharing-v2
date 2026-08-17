package com.zanh.route_sharing.dto.trip.dropoffarrival;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TripDropoffArrivalRequest(
                @NotNull(message = "currentLocation không được trống.") @Valid TripDropoffArrivalLocationRequest currentLocation) {
}
