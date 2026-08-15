package com.zanh.route_sharing.repository.sharedroute.triplocation.model;

import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

public record TripLocationCommitCommand(
        Long actorId,
        Long tripId,
        Point position,
        Instant observedAt,
        Instant receivedAt,
        BigDecimal accuracyMeters) {
}
