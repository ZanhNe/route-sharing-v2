package com.zanh.route_sharing.dto.sharedroute.preview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PreviewSharedRouteRequest(
                @NotNull @Positive Long schoolId,

                @NotNull @Valid PreviewPointRequest pickup,

                @NotNull @Valid PreviewPointRequest passengerDestination) {
}
