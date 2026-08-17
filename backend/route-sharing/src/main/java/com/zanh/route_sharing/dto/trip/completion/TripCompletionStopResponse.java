package com.zanh.route_sharing.dto.trip.completion;

import java.time.Instant;

public record TripCompletionStopResponse(
                Long stopId,
                Integer order,
                String status,
                Instant completedAt) {
}
