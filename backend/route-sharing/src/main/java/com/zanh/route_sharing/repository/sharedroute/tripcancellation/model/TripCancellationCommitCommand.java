package com.zanh.route_sharing.repository.sharedroute.tripcancellation.model;

import java.time.Instant;

public record TripCancellationCommitCommand(
        Long actorId,
        Long tripId,
        String reason,
        Instant cancelledAt) {
}
