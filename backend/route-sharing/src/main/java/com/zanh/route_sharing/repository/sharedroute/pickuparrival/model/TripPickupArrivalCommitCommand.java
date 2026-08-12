package com.zanh.route_sharing.repository.sharedroute.pickuparrival.model;

import org.locationtech.jts.geom.Point;

import java.time.Instant;

public record TripPickupArrivalCommitCommand(
        Long actorId,
        Long tripId,
        Point currentLocation,
        Instant arrivedAt) {
}
