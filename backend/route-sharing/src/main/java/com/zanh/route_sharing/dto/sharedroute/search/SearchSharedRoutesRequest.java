package com.zanh.route_sharing.dto.sharedroute.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record SearchSharedRoutesRequest(
        @NotNull
        @Positive
        Long schoolId,

        @NotNull
        @Valid
        SearchPointRequest pickup,

        @NotNull
        @Valid
        SearchPointRequest destination,

        @NotNull
        Instant desiredDepartureTime
) {
}
