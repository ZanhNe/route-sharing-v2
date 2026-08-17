package com.zanh.route_sharing.dto.trip.completion;

import java.time.Instant;

public record TripCompletionResponse(
                Long tripId,
                Long routeId,
                String tripStatus,
                Instant endedAt,
                Integer actualPassengerCount,
                TripCompletionStopResponse driverEnd) {
}
