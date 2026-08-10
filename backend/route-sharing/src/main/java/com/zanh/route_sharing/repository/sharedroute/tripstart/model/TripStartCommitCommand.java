package com.zanh.route_sharing.repository.sharedroute.tripstart.model;

import org.locationtech.jts.geom.Point;

import java.time.Instant;

public record TripStartCommitCommand(
                Long actorId,
                Long tripId,
                Point currentLocation,
                Instant startedAt) {
}
