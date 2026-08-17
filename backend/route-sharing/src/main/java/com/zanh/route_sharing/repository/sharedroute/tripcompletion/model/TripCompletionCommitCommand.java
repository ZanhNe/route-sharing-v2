package com.zanh.route_sharing.repository.sharedroute.tripcompletion.model;

import org.locationtech.jts.geom.Point;

public record TripCompletionCommitCommand(Long actorId, Long tripId, Point currentLocation) {
}
